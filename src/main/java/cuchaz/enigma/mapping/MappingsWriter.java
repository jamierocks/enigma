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

import com.beust.jcommander.internal.Sets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MappingsWriter {

	private Set<FieldReference> fields = Sets.newHashSet();
	private Set<MethodReference> methods = Sets.newHashSet();
	
	public void write(Writer out, Mappings mappings) throws IOException {
		write(new PrintWriter(out), mappings);
	}
	
	public void write(PrintWriter out, Mappings mappings) throws IOException {
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			write(out, mappings, classMapping);
		}
		for (FieldReference fieldReference : this.fields) {
			fieldReference.write(out);
		}
		for (MethodReference methodReference : this.methods) {
			methodReference.write(out);
		}
	}
	
	private void write(PrintWriter out, Mappings mappings, ClassMapping classMapping) throws IOException {
		if (classMapping.getDeobfName() != null) {
			out.format("CL: %s %s\n", classMapping.getObfFullName(), classMapping.getDeobfName());
		}
		
		for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
			write(out, mappings, innerClassMapping);
		}
		
		for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
			write(mappings, classMapping, fieldMapping);
		}
		
		for (MethodMapping methodMapping : sorted(classMapping.methods())) {
			write(mappings, classMapping, methodMapping);
		}
	}
	
	private void write(Mappings mappings, ClassMapping classMapping, FieldMapping fieldMapping) throws IOException {
		this.fields.add(new FieldReference(mappings, classMapping, fieldMapping));
	}
	
	private void write(Mappings mappings, ClassMapping classMapping, MethodMapping methodMapping) throws IOException {
		this.methods.add(new MethodReference(mappings, classMapping, methodMapping));
	}
	
	private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
		List<T> out = new ArrayList<>();
		for (T t : classes) {
			out.add(t);
		}
		Collections.sort(out);
		return out;
	}

	private static class FieldReference {

		public final Mappings mappings;
		public final ClassMapping classMapping;
		public final FieldMapping fieldMapping;

		public FieldReference(Mappings mappings, ClassMapping classMapping, FieldMapping fieldMapping) {
			this.mappings = mappings;
			this.classMapping = classMapping;
			this.fieldMapping = fieldMapping;
		}

		protected void write(PrintWriter out) {
			String classDeobfName = classMapping.getObfFullName();

			if (classMapping.getDeobfName() != null) {
				classDeobfName = classMapping.getDeobfName();
			}

			out.format("FD: %s/%s %s/%s\n",
					classMapping.getObfFullName(), fieldMapping.getObfName(),
					classDeobfName, fieldMapping.getDeobfName());
		}
	}

	private static class MethodReference {

		public final Mappings mappings;
		public final ClassMapping classMapping;
		public final MethodMapping methodMapping;

		public MethodReference(Mappings mappings, ClassMapping classMapping, MethodMapping methodMapping) {
			this.mappings = mappings;
			this.classMapping = classMapping;
			this.methodMapping = methodMapping;
		}

		protected void write(PrintWriter out) {
			String classDeobfName = classMapping.getObfFullName();

			if (classMapping.getDeobfName() != null) {
				classDeobfName = classMapping.getDeobfName();
			}

			if (!methodMapping.getObfName().equals("<init>")) {
				out.format("MD: %s/%s %s %s/%s %s\n",
						classMapping.getObfFullName(), methodMapping.getObfName(), methodMapping.getObfSignature(),
						classDeobfName, methodMapping.getDeobfName(), methodMapping.getDeobfSiganture(mappings));
			}
		}
	}
}
