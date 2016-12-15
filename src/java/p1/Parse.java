package p1;

import java.io.*;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.SAXException;

/*
 * Parse Class 
 */
public class Parse {

    /*
     * Parses a docFile into text file using Apache TIKA API.
     */
    public static File parseToText(File file) throws IOException, SAXException, FileNotFoundException, TikaException {

        String name = file.getName() + ".txt";
        File someFile = new File("C://temp//converted", name);
        //If file doesn't already exist, parse it.
        if (!someFile.exists()) {
            FileWriter fw = new FileWriter(someFile);
            OfficeParser p = new OfficeParser();
            TikaInputStream stream = TikaInputStream.get(file);
            ContentHandler handler = new ToTextContentHandler();
            Metadata metadata = new Metadata();
            p.parse(stream, handler, metadata, new ParseContext());
            String text = handler.toString();
            fw.write(text);
            fw.close();
        }
        return someFile;
    }
}
