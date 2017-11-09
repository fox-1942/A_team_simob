package hu.iit.uni.miskolc.gml.editor.service.impl;

import hu.iit.uni.miskolc.gml.editor.model.Export;
import hu.iit.uni.miskolc.gml.editor.model.Import;
import net.opengis.indoorgml.core.v_1_0.IndoorFeaturesType;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by fox on 2017.08.16..
 */
public class ServiceFacade {

    private Export indoorGMLexport = new ExportImpl();

    private Import indoorGMLImport = new ImportImpl();

    public ServiceFacade() {
    }


    public IndoorFeaturesType unmarshalmax(File inputFile) throws JAXBException {

        return indoorGMLImport.unmarshalmax(inputFile);
    }

    public void marshalMax(File outputFile) throws JAXBException {
        indoorGMLexport.marshalMax(outputFile);
    }

    public void drawGmlFile() throws IOException, SAXException, ParserConfigurationException {
    indoorGMLImport.drawGmlFile();

    }

}
