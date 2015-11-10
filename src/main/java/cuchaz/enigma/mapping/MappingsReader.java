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
package cuchaz.enigma.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Deque;
import java.util.Scanner;

import com.google.common.collect.Queues;

public class MappingsReader {
	
	public Mappings read(Reader in)
	throws IOException, MappingParseException {
		return read(new BufferedReader(in));
	}
	
	public Mappings read(BufferedReader in)
	throws IOException, MappingParseException {
		Mappings mappings = new Mappings();
		Deque<Object> mappingStack = Queues.newArrayDeque();
		
		Scanner scanner = new Scanner(in);

		while (scanner.hasNext()) {
			String line = scanner.nextLine();

			String[] split = line.substring(4).split(" ");
			if (line.startsWith("CL: ")) {
				mappings.addClassMapping(new ClassMapping(split[0], split[1]));
			} else if (line.startsWith("FD: ")) {
				String[] deobfSplit = split[1].split("/");
				String deobfClass = split[1]
						.substring(0, split[1].length() - (deobfSplit[deobfSplit.length - 1].length() + 1));
				//mappings.getClassByDeobf(deobfClass).addFieldMapping(new FieldMapping());
				// TODO:
			} else if (line.startsWith("MD: ")) {
				// TODO:
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
