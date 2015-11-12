package cuchaz.enigma.mapping.reader;

import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public interface IMappingsReader {

    Mappings read(Reader in) throws IOException, MappingParseException;

    Mappings read(BufferedReader in) throws IOException, MappingParseException;



}
