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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MappingsWriter {
	
	public void write(Writer out, Mappings mappings) throws IOException {
		write(new PrintWriter(out), mappings);
	}
	
	public void write(PrintWriter out, Mappings mappings) throws IOException {
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			write(out, classMapping);
		}
	}
	
	private void write(PrintWriter out, ClassMapping classMapping) throws IOException {
		if (classMapping.getDeobfName() != null) {
			out.format("CL: %s %s\n", classMapping.getObfFullName(), classMapping.getDeobfName());
		}
		
		for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
			write(out, innerClassMapping);
		}
		
		for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
			write(out, classMapping, fieldMapping);
		}
		
		for (MethodMapping methodMapping : sorted(classMapping.methods())) {
			write(out, classMapping, methodMapping);
		}
	}
	
	private void write(PrintWriter out, ClassMapping classMapping, FieldMapping fieldMapping) throws IOException {
		out.format("FD: %s/%s %s/%s\n", classMapping.getObfFullName(), fieldMapping.getObfName(),
				classMapping.getDeobfName(), fieldMapping.getDeobfName());
	}
	
	private void write(PrintWriter out, ClassMapping classMapping, MethodMapping methodMapping) throws IOException {
		out.format("MD: %s/%s %s %s/%s %s",
				classMapping.getObfFullName(), methodMapping.getObfName(), methodMapping.getObfSignature(),
				classMapping.getDeobfName(), methodMapping.getDeobfName(), methodMapping.getObfSignature());
		// TODO: 6th param might break things :\
	}
	
	private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
		List<T> out = new ArrayList<T>();
		for (T t : classes) {
			out.add(t);
		}
		Collections.sort(out);
		return out;
	}
}
