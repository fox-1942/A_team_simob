package hu.iit.uni.miskolc.gml.editor.gui;

import hu.iit.uni.miskolc.gml.editor.model.CellSpace;
import hu.iit.uni.miskolc.gml.editor.service.impl.CellSpaceImportImpl;
import hu.iit.uni.miskolc.gml.editor.service.impl.ServiceFacade;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.ArrayUtils;
import org.geotools.math.Statistics;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javafx.scene.input.MouseEvent;


/**
 * Created by fox on 2017.08.18..
 */
public class MainWindowController {

    private double initX;
    private double initY;
    private ServiceFacade facade;
    private File outputFile;
    private String path;
    private CellSpaceImportImpl cellSpaceImport=new CellSpaceImportImpl();
    private Group root;

    public MainWindowController() {
        facade = new ServiceFacade();
    }

    public File createOutputFile() {
        outputFile = new File(path);
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        System.out.println(outputFile.exists() + outputFile.getAbsolutePath());
        return outputFile;
    }

    public void showSaveDialog(){
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile != null) {
            System.out.println("File selected");
            path = selectedFile.getPath();
            System.out.println(path);
        } else {
            System.out.println("Cancelled.");
        }
    }

    public void marshal(ActionEvent event) throws JAXBException {
            showSaveDialog();   //Path is set.
            facade.marshalMax(createOutputFile());
    }


    //-------------------------------------------------------------------------------

    public void showSingleFileChooser() {

        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            System.out.println("File selected");
            path = selectedFile.getPath();
            System.out.println(path);
        } else {
            System.out.println("Cancelled.");
        }
    }


    //-------------------------------------------------------------------------------

    public void unmarshal(ActionEvent event) {
        try {
            showSingleFileChooser();  // Path is set.
            File inputFile = new File(path);
            facade.unmarshalmax(inputFile);
            System.out.println(inputFile.exists());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    //--------------------------------------------------------------------------------

    public void drawGmlFile() throws ParserConfigurationException, SAXException, IOException {
            //showSingleFileChooser();  // Path is set.
            //File inputFile = new File(path);
            facade.domImport();
           // System.out.println(inputFile.exists());
        }

    //--------------------------------------------------------------------------------


    public void readGMLFile() throws ParserConfigurationException, SAXException, IOException {
        //showSingleFileChooser();  // Path is set.
        //File inputFile = new File(path);
        facade.cellSpaceCreator();
        // System.out.println(inputFile.exists());
    }

    //--------------------------------------------------------------------------------


    // Creating circle -------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------


    private Circle createCircle(final Color color, double radius, double x, double y) {
        //create a circle with desired name,  color and radius
        final Circle circle = new Circle(x,y,radius);

        //add a shadow effect
        circle.setEffect(new InnerShadow(1, color.brighter()));

        //change a cursor when it is over circle
        circle.setCursor(Cursor.CROSSHAIR);

        circle.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                //when mouse is pressed, store initial position
                initX = circle.getTranslateX();
                initY = circle.getTranslateY();
            }
        });

        circle.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                double dragX = me.getSceneX();
                double dragY = me.getSceneY();
                //calculate new position of the circle
                double newXPosition = initX + dragX;
                double newYPosition = initY + dragY;

                //if new position do not exceeds borders of the rectangle, translate to this position


                root.setLayoutX(600);
                root.setLayoutY(300);

                    circle.setCenterX(newXPosition);
                    circle.setCenterY(newYPosition);
                }

        });

        circle.setOnMouseEntered(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                //change the z-coordinate of the circle
                circle.toFront();
            }
        });

        circle.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                //when mouse is pressed, store initial position
                initX = circle.getTranslateX();
                initY = circle.getTranslateY();
            }
        });


        return circle;
    }


    // ---------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------

    public Group createMeshView() {
//        ArrayList<CellSpace> cellSpaceImportList = cellSpaceImport.cellSpaceCreator();
//        Group root = new Group();
//
//            for (CellSpace cp: cellSpaceImportList) {
//                Polyline rectangle = new Polyline();
//                ArrayList doubles=new ArrayList<Double>();
//
//            for(int j=0;j<cp.getCellSpaceFloorCoordinateArrayList().size();j++){
//
//                    doubles.add(cp.getCellSpaceFloorCoordinateArrayList().get(j).getCoordinateX());
//                    doubles.add(cp.getCellSpaceFloorCoordinateArrayList().get(j).getCoordinateY());
//
//                }
//
//                double[] array = ArrayUtils.toPrimitive((Double[]) doubles.toArray(new Double[doubles.size()]));
//                rectangle = new Polyline(array);
//                rectangle.setStrokeWidth(0.3);
//                rectangle.setStroke(Color.DARKRED);
//
//                root.getChildren().add(rectangle);
//            }
//        return root;
//        }

        ArrayList<CellSpace> cellSpaceImportList = cellSpaceImport.cellSpaceCreator();
        Group root = new Group();

        for (CellSpace cp : cellSpaceImportList) {
            ArrayList<Circle> circleArrayList=new ArrayList<Circle>();

            for (int j = 0; j < cp.getCellSpaceFloorCoordinateArrayList().size(); j++) {
                circleArrayList.add(j, createCircle(Color.GREENYELLOW, 0.4, cp.getCellSpaceFloorCoordinateArrayList().get(j).getCoordinateX(),
                        cp.getCellSpaceFloorCoordinateArrayList().get(j).getCoordinateY()));
            }

            for (int i=0;i<circleArrayList.size()-1;i++) {

                Line line = new Line();
                line.setStroke(Color.BLUE);
                line.setStrokeWidth(0.3);

                if(i==circleArrayList.size()-2){
                    line.startXProperty().bind(circleArrayList.get(i).centerXProperty());
                    line.startYProperty().bind(circleArrayList.get(i).centerYProperty());
                    line.endXProperty().bind(circleArrayList.get(0).centerXProperty());
                    line.endYProperty().bind(circleArrayList.get(0).centerYProperty());
                    root.getChildren().add(circleArrayList.get(i));
                    root.getChildren().add(line);
                    break;
                }

                line.startXProperty().bind(circleArrayList.get(i).centerXProperty());
                line.startYProperty().bind(circleArrayList.get(i).centerYProperty());
                line.endXProperty().bind(circleArrayList.get(i+1).centerXProperty());
                line.endYProperty().bind(circleArrayList.get(i+1).centerYProperty());

                root.getChildren().add(circleArrayList.get(i));
                root.getChildren().add(line);
            }
        }
        return root;
    }


    public SubScene getSubScene(Point3D rotationAxis) {

        root=createMeshView();

        root.setLayoutX(500);
        root.setLayoutY(200);
        root.setTranslateX(0);
        root.setTranslateY(0);
        root.setScaleX(10);
        root.setScaleY(10);
        //root.prefHeight(10);
        //root.prefWidth(10);
        SubScene ss = new SubScene(root, 800, 500, false, SceneAntialiasing.BALANCED);
        return ss;
    }
}