/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping.io.reader;

import com.google.common.collect.Queues;
import cuchaz.enigma.mapping.type.ArgumentMapping;
import cuchaz.enigma.mapping.type.ClassMapping;
import cuchaz.enigma.mapping.type.FieldMapping;
import cuchaz.enigma.mapping.exception.MappingParseException;
import cuchaz.enigma.mapping.util.Mappings;
import cuchaz.enigma.mapping.type.MethodMapping;
import cuchaz.enigma.mapping.element.Signature;
import cuchaz.enigma.mapping.element.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Deque;

public class MappingsLegacyReader implements IMappingsReader {

	@Override
	public Mappings read(Reader in) throws IOException, MappingParseException {
		return read(new BufferedReader(in));
	}

	@Override
	public Mappings read(BufferedReader in) throws IOException, MappingParseException {
		Mappings mappings = new Mappings();
		Deque<Object> mappingStack = Queues.newArrayDeque();

		int lineNumber = 0;
		String line = null;
		while ( (line = in.readLine()) != null) {
			lineNumber++;

			// strip comments
			int commentPos = line.indexOf('#');
			if (commentPos >= 0) {
				line = line.substring(0, commentPos);
			}

			// skip blank lines
			if (line.trim().length() <= 0) {
				continue;
			}

			// get the indent of this line
			int indent = 0;
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) != '\t') {
					break;
				}
				indent++;
			}

			// handle stack pops
			while (indent < mappingStack.size()) {
				mappingStack.pop();
			}

			String[] parts = line.trim().split("\\s");
			try {
				// read the first token
				String token = parts[0];

				if (token.equalsIgnoreCase("CLASS")) {
					ClassMapping classMapping;
					if (indent <= 0) {
						// outer class
						classMapping = readClass(parts, false);
						mappings.addClassMapping(classMapping);
					} else {

						// inner class
						if (!(mappingStack.peek() instanceof ClassMapping)) {
							throw new MappingParseException(lineNumber, "Unexpected CLASS entry here!");
						}

						classMapping = readClass(parts, true);
						((ClassMapping)mappingStack.peek()).addInnerClassMapping(classMapping);
					}
					mappingStack.push(classMapping);
				} else if (token.equalsIgnoreCase("FIELD")) {
					if (mappingStack.isEmpty() || ! (mappingStack.peek() instanceof ClassMapping)) {
						throw new MappingParseException(lineNumber, "Unexpected FIELD entry here!");
					}
					((ClassMapping)mappingStack.peek()).addFieldMapping(readField(parts));
				} else if (token.equalsIgnoreCase("METHOD")) {
					if (mappingStack.isEmpty() || ! (mappingStack.peek() instanceof ClassMapping)) {
						throw new MappingParseException(lineNumber, "Unexpected METHOD entry here!");
					}
					MethodMapping methodMapping = readMethod(parts);
					((ClassMapping)mappingStack.peek()).addMethodMapping(methodMapping);
					mappingStack.push(methodMapping);
				} else if (token.equalsIgnoreCase("ARG")) {
					if (mappingStack.isEmpty() || ! (mappingStack.peek() instanceof MethodMapping)) {
						throw new MappingParseException(lineNumber, "Unexpected ARG entry here!");
					}
					((MethodMapping)mappingStack.peek()).addArgumentMapping(readArgument(parts));
				}
			} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				throw new MappingParseException(lineNumber, "Malformed line:\n" + line);
			}
		}

		return mappings;
	}

	private ArgumentMapping readArgument(String[] parts) {
		return new ArgumentMapping(Integer.parseInt(parts[1]), parts[2]);
	}

	private ClassMapping readClass(String[] parts, boolean makeSimple) {
		if (parts.length == 2) {
			return new ClassMapping(parts[1]);
		} else {
			return new ClassMapping(parts[1], parts[2]);
		}
	}

	/* TEMP */
	protected FieldMapping readField(String[] parts) {
		return new FieldMapping(parts[1], new Type(parts[3]), parts[2]);
	}

	private MethodMapping readMethod(String[] parts) {
		if (parts.length == 3) {
			return new MethodMapping(parts[1], new Signature(parts[2]));
		} else {
			return new MethodMapping(parts[1], new Signature(parts[3]), parts[2]);
		}
	}
}