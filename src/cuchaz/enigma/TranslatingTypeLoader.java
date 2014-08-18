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
package cuchaz.enigma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import com.beust.jcommander.internal.Maps;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import cuchaz.enigma.analysis.BridgeFixer;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.ClassTranslator;
import cuchaz.enigma.bytecode.InnerClassWriter;
import cuchaz.enigma.bytecode.MethodParameterWriter;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Translator;

public class TranslatingTypeLoader implements ITypeLoader
{
	private JarFile m_jar;
	private JarIndex m_jarIndex;
	private Translator m_obfuscatingTranslator;
	private Translator m_deobfuscatingTranslator;
	private Map<String,byte[]> m_cache;
	
	public TranslatingTypeLoader( JarFile jar, JarIndex jarIndex, Translator obfuscatingTranslator, Translator deobfuscatingTranslator )
	{
		m_jar = jar;
		m_jarIndex = jarIndex;
		m_obfuscatingTranslator = obfuscatingTranslator;
		m_deobfuscatingTranslator = deobfuscatingTranslator;
		m_cache = Maps.newHashMap();
	}
	
	@Override
	public boolean tryLoadType( String deobfClassName, Buffer out )
	{
		// check the cache
		byte[] data;
		if( m_cache.containsKey( deobfClassName ) )
		{
			data = m_cache.get( deobfClassName );
		}
		else
		{
			data = loadType( deobfClassName );
			m_cache.put( deobfClassName, data );
		}
		
		if( data == null )
		{
			return false;
		}
		
		// send the class to the decompiler
		out.reset( data.length );
		System.arraycopy( data, 0, out.array(), out.position(), data.length );
		out.position( 0 );
		return true;
	}
	
	private byte[] loadType( String deobfClassName )
	{
		// what class file should we actually load?
		ClassEntry deobfClassEntry = new ClassEntry( deobfClassName );
		ClassEntry obfClassEntry = m_obfuscatingTranslator.translateEntry( deobfClassEntry );
		
		// is this an inner class referenced directly?
		if( m_jarIndex.getOuterClass( obfClassEntry.getName() ) != null )
		{
			// this class doesn't really exist. Reference it by outer$inner instead
			System.err.println( String.format( "WARNING: class %s referenced by bare inner name", deobfClassName ) );
			return null;
		}
		
		/* DEBUG
		if( !Arrays.asList( "java", "org", "io" ).contains( deobfClassName.split( "/" )[0] ) )
		{
			System.out.println( String.format( "Looking for %s (%s)", deobfClassEntry.getName(), obfClassEntry.getName() ) );
		}
		*/
		
		// get the jar entry
		String classFileName;
		if( obfClassEntry.isInnerClass() )
		{
			classFileName = obfClassEntry.getInnerClassName();
		}
		else
		{
			classFileName = obfClassEntry.getOuterClassName();
		}
		JarEntry entry = m_jar.getJarEntry( classFileName + ".class" );
		if( entry == null )
		{
			return null;
		}
		
		try
		{
			// read the class file into a buffer
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			byte[] buf = new byte[1024*1024]; // 1 KiB
			InputStream in = m_jar.getInputStream( entry );
			while( true )
			{
				int bytesRead = in.read( buf );
				if( bytesRead <= 0 )
				{
					break;
				}
				data.write( buf, 0, bytesRead );
			}
			data.close();
			in.close();
			buf = data.toByteArray();
			
			// load the javassist handle to the raw class
			String javaClassFileName = Descriptor.toJavaName( classFileName );
			ClassPool classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( javaClassFileName, buf ) );
			CtClass c = classPool.get( javaClassFileName );
			
			// reconstruct inner classes
			new InnerClassWriter( m_jarIndex ).write( c );
			
			// re-get the javassist handle since we changed class names
			String javaClassReconstructedName = Descriptor.toJavaName( obfClassEntry.getName() );
			classPool = new ClassPool();
			classPool.insertClassPath( new ByteArrayClassPath( javaClassReconstructedName, c.toBytecode() ) );
			c = classPool.get( javaClassReconstructedName );
			
			// check that the file is correct after inner class reconstruction (ie cause Javassist to fail fast if something is wrong)
			assertClassName( c, obfClassEntry );
			
			// do all kinds of deobfuscating transformations on the class
			new BridgeFixer().fixBridges( c );
			new MethodParameterWriter( m_deobfuscatingTranslator ).writeMethodArguments( c );
			new ClassTranslator( m_deobfuscatingTranslator ).translate( c );
			
			// sanity checking
			assertClassName( c, deobfClassEntry );
			
			// we have a transformed class!
			return c.toBytecode();
		}
		catch( IOException | NotFoundException | CannotCompileException ex )
		{
			throw new Error( ex );
		}
	}

	private void assertClassName( CtClass c, ClassEntry obfClassEntry )
	{
		String name1 = Descriptor.toJvmName( c.getName() );
		assert( name1.equals( obfClassEntry.getName() ) )
			: String.format( "Looking for %s, instead found %s", obfClassEntry.getName(), name1 );
		
		String name2 = Descriptor.toJvmName( c.getClassFile().getName() );
		assert( name2.equals( obfClassEntry.getName() ) )
			: String.format( "Looking for %s, instead found %s", obfClassEntry.getName(), name2 );
	}
}
