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
import java.util.Scanner;

public class MappingsReader {

	public Mappings read(Reader in)
	throws IOException, MappingParseException {
		return read(new BufferedReader(in));
	}

	public Mappings read(BufferedReader in)
	throws IOException, MappingParseException {
		Mappings mappings = new Mappings();

		Scanner scanner = new Scanner(in);

		while (scanner.hasNext()) {
			String line = scanner.nextLine();

			String[] split = line.substring(4).split(" ");
			if (line.startsWith("CL: ")) {
				mappings.addClassMapping(new ClassMapping(split[0], split[1]));
			} else if (line.startsWith("FD: ")) {
				String[] obfSplit = split[0].split("/");

				String[] deobfSplit = split[1].split("/");
				String deobfClass = split[1]
						.substring(0, split[1].length() - (deobfSplit[deobfSplit.length - 1].length() + 1));

				mappings.getClassByDeobf(deobfClass)
						.addFieldMapping(new FieldMapping(obfSplit[obfSplit.length - 1],
								new Type("B"), deobfSplit[deobfSplit.length - 1]));
			} else if (line.startsWith("MD: ")) {
				String[] obfSplit = split[0].split("/");

				String[] deobfSplit = split[2].split("/");
				String deobfClass = split[2]
						.substring(0, split[2].length() - (deobfSplit[deobfSplit.length - 1].length() + 1));

				mappings.getClassByDeobf(deobfClass)
						.addMethodMapping(new MethodMapping(obfSplit[obfSplit.length - 1],
								new Signature(split[1]), deobfSplit[deobfSplit.length - 1]));
			}
		}

		return mappings;
	}
}
