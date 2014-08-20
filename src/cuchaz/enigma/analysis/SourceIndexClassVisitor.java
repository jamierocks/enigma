/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ConstructorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MethodEntry;

public class SourceIndexClassVisitor extends SourceIndexVisitor
{
	private ClassEntry m_classEntry;
	private Multiset<Entry> m_indices;
	
	public SourceIndexClassVisitor( ClassEntry classEntry )
	{
		m_classEntry = classEntry;
		m_indices = HashMultiset.create();
	}
	
	@Override
	public Void visitTypeDeclaration( TypeDeclaration node, SourceIndex index )
	{
		return recurse( node, index );
	}
	
	@Override
	public Void visitSimpleType( SimpleType node, SourceIndex index )
	{
		TypeReference ref = node.getUserData( Keys.TYPE_REFERENCE );
		if( node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY )
		{
			ClassEntry classEntry = new ClassEntry( ref.getInternalName() );
			index.addReference(
				node.getIdentifierToken(),
				new EntryReference<Entry,Entry>( classEntry, m_classEntry, m_indices.count( classEntry ) )
			);
		}
		
		return recurse( node, index );
	}
	
	@Override
	public Void visitMethodDeclaration( MethodDeclaration node, SourceIndex index )
	{
		MethodDefinition def = node.getUserData( Keys.METHOD_DEFINITION );
		ClassEntry classEntry = new ClassEntry( def.getDeclaringType().getInternalName() );
		MethodEntry methodEntry = new MethodEntry( classEntry, def.getName(), def.getSignature() );
		index.addDeclaration( node.getNameToken(), methodEntry );
		//if( !def.getName().equals( "<clinit>" ) )
		
		return node.acceptVisitor( new SourceIndexBehaviorVisitor( methodEntry ), index );
	}
	
	@Override
	public Void visitConstructorDeclaration( ConstructorDeclaration node, SourceIndex index )
	{
		MethodDefinition def = node.getUserData( Keys.METHOD_DEFINITION );
		ClassEntry classEntry = new ClassEntry( def.getDeclaringType().getInternalName() );
		ConstructorEntry constructorEntry = new ConstructorEntry( classEntry, def.getSignature() );
		index.addDeclaration( node.getNameToken(), constructorEntry );
		
		return recurse( node, index );
	}
	
	@Override
	public Void visitFieldDeclaration( FieldDeclaration node, SourceIndex index )
	{
		FieldDefinition def = node.getUserData( Keys.FIELD_DEFINITION );
		ClassEntry classEntry = new ClassEntry( def.getDeclaringType().getInternalName() );
		FieldEntry fieldEntry = new FieldEntry( classEntry, def.getName() );
		assert( node.getVariables().size() == 1 );
		VariableInitializer variable = node.getVariables().firstOrNullObject();
		index.addDeclaration( variable.getNameToken(), fieldEntry );
		
		return recurse( node, index );
	}
	
	@Override
	public Void visitEnumValueDeclaration( EnumValueDeclaration node, SourceIndex index )
	{
		// treat enum declarations as field declarations
		FieldDefinition def = node.getUserData( Keys.FIELD_DEFINITION );
		ClassEntry classEntry = new ClassEntry( def.getDeclaringType().getInternalName() );
		FieldEntry fieldEntry = new FieldEntry( classEntry, def.getName() );
		index.addDeclaration( node.getNameToken(), fieldEntry );
		
		return recurse( node, index );
	}
}