package edu.pnu.gui;

import edu.pnu.project.*;
import edu.pnu.util.GeometryUtil;
import edu.pnu.util.JTSUtil;
import net.opengis.indoorgml.core.*;
import net.opengis.indoorgml.geometry.LineString;
import net.opengis.indoorgml.geometry.LinearRing;
import net.opengis.indoorgml.geometry.Point;
import net.opengis.indoorgml.geometry.Polygon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CanvasPanel extends JPanel implements MouseListener, MouseMotionListener,
        MouseWheelListener, KeyListener {
    private MainFrame mainFrame = null;

    private ProjectFile project;

    private State stateEnd1 = null;

    private State stateEnd2 = null;

    private SpaceLayer spaceLayerEnd1 = null;

    private SpaceLayer spaceLayerEnd2 = null;

    private ArrayList<State> statesEnd1 = new ArrayList<State>();

    private ArrayList<State> statesEnd2 = new ArrayList<State>();

    private ArrayList<Point> transitionPoints = new ArrayList<Point>();

    private State selectedState = null;

    private HashMap<State, Color> selectedStateMap = new HashMap<State, Color>();

    private Transition selectedTransition = null;

    private HashMap<Transition, Color> selectedTransitionMap = new HashMap<Transition, Color>();

    private CellSpace selectedCellSpace = null;

    private HashMap<CellSpace, Color> selectedCellSpaceMap = new HashMap<CellSpace, Color>();

    private CellSpaceBoundary selectedCellSpaceBoundary = null;

    private HashMap<CellSpaceBoundary, Color> selectedCellSpaceBoundaryMap = new HashMap<CellSpaceBoundary, Color>();

    // //// for create cellspace
    // private ArrayList<Point> cellSpacePoints = new ArrayList<Point>();
    // / for create cellspace
    private ArrayList<LineString> cellSpaceCreatingLineStrings = new ArrayList<LineString>();

    private ArrayList<Point> snapPointsToCreatingCellSpace = new ArrayList<Point>();

    // /

    // for creating door
    private ArrayList<Point> doorPointList = new ArrayList<Point>();
    private LineString baseDoorLine = null;

    private BufferedImage floorPlan = null;

    private int floorPlanWidth = 0;

    private int floorPlanHeight = 0;

    private double floorPlanScale = 1.0;

    private int previousMouseX;

    private int previousMouseY;

    private boolean isMouseDown = false;

    private int currentKeyEvent = KeyEvent.KEY_RELEASED;

    private JPopupMenu popupMenu_State;

    private JMenuItem mntmStateDuality;

    private JPopupMenu popupMenu_CellSpace;

    private JMenuItem mntmCellSpaceDuality;

    private JPopupMenu popupMenu_Transition;

    private JMenuItem mntmTransitionDuality;

    private JPopupMenu popupMenu_CellSpaceBoundary;

    private JMenuItem mntmCellSpaceBoundaryDuality;

    private JMenuItem mntmStateProperties;

    private JMenuItem mntmCellSpaceProperties;

    private JMenuItem mntmTransitionProperties;

    private JMenuItem mntmCellSpaceBoundaryProperties;

    /**
     * Create the panel.
     */
    public CanvasPanel() {
        addPopup(this, getPopupMenu_State());
        add(getPopupMenu_CellSpace());
        add(getPopupMenu_Transition());
        add(getPopupMenu_CellSpaceBoundary());
    }

    public CanvasPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        this.addKeyListener(this);
        setLayout(null);

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
        int currentMouseX = e.getX();
        int currentMouseY = e.getY();

        if (!SwingUtilities.isLeftMouseButton(e))
            return;

        EditState currentEditState = project.getEditState();
        EditWorkState currentEditWorkState = project.getEditWorkState();
        if (currentEditState == EditState.SELECT_STATE || currentEditState == EditState.MOVE_STATE) {
            for (State state : selectedStateMap.keySet()) {
                movePoint(state.getPosition(), previousMouseX, previousMouseY, currentMouseX,
                        currentMouseY);
            }

            project.setEditState(EditState.MOVE_STATE);
        } else if (currentEditState == EditState.SELECT_CELLSPACE
                || currentEditState == EditState.MOVE_CELLSPACE) {

        } else if (currentEditState == EditState.CREATE_CELLSPACE
                && snapPointsToCreatingCellSpace.size() > 0) {
            // cellSpaceLineString의 각 LineString과 event Point가 동일한것 찾음 -> 인접한 LineString의 공통된 Point는 좌표는 같지만 다른 객체이므로 2개씩 찾아진다.
            // 점이 두개만 찍혔을 때는 1개씩 찾아진다.
            for (Point point : snapPointsToCreatingCellSpace) {
                movePoint(point, previousMouseX, previousMouseY, currentMouseX, currentMouseY);
            }

            Point snapPointToCellSpace = searchSnapPointToCellSpace(e);
            if (snapPointToCellSpace != null) {
                for (Point point : snapPointsToCreatingCellSpace) {
                    point.setPanelRatioX(snapPointToCellSpace.getPanelRatioX());
                    point.setPanelRatioY(snapPointToCellSpace.getPanelRatioY());
                }
            } else if (snapPointToCellSpace == null && currentKeyEvent != KeyEvent.VK_ALT) {
                int size = cellSpaceCreatingLineStrings.size();
                Point firstPoint = snapPointsToCreatingCellSpace.get(0);
                if (size == 1) {
                    LineString ls = cellSpaceCreatingLineStrings.get(0);
                    if(ls.getPoints().size() > 1) {
                        Point otherP = null;
                        if (ls.getPoints().indexOf(firstPoint) == 0) {
                            otherP = ls.getPoints().get(1);
                        } else {
                            otherP = ls.getPoints().get(0);
                        }
                        if (Math.abs(otherP.getPanelX() - firstPoint.getPanelX()) <= 5) {
                            firstPoint.setPanelX(otherP.getPanelX());
                        }
                        if (Math.abs(otherP.getPanelY() - firstPoint.getPanelY()) <= 5) {
                            firstPoint.setPanelY(otherP.getPanelY());
                        }
                        setPanelRatioXY(firstPoint);
                    }
                } else {
                    Point otherP1 = null;
                    Point otherP2 = null;
                    LineString otherLS = null;
                    for (int i = 0; i < size; i++) {
                        LineString ls = cellSpaceCreatingLineStrings.get(i);
                        if (ls.getPoints().contains(firstPoint)) {
                            if (ls.getPoints().indexOf(firstPoint) == 0) {
                                otherP2 = ls.getPoints().get(1);
                                if (i == 0)
                                    otherLS = cellSpaceCreatingLineStrings.get(size - 1);
                                else
                                    otherLS = cellSpaceCreatingLineStrings.get(i - 1);
                                otherP1 = otherLS.getPoints().get(0);
                            } else {
                                otherP1 = ls.getPoints().get(0);
                                otherLS = cellSpaceCreatingLineStrings.get((i + 1) % size);
                                otherP2 = otherLS.getPoints().get(1);
                            }
                            for (Point point : snapPointsToCreatingCellSpace) {
                                if (Math.abs(otherP1.getPanelX() - point.getPanelX()) <= 5) {
                                    point.setPanelX(otherP1.getPanelX());
                                }
                                if (Math.abs(otherP1.getPanelY() - point.getPanelY()) <= 5) {
                                    point.setPanelY(otherP1.getPanelY());
                                }
                                if (Math.abs(otherP2.getPanelX() - point.getPanelX()) <= 5) {
                                    point.setPanelX(otherP2.getPanelX());
                                }
                                if (Math.abs(otherP2.getPanelY() - point.getPanelY()) <= 5) {
                                    point.setPanelY(otherP2.getPanelY());
                                }
                                setPanelRatioXY(point);
                            }
                        }
                    }
                }
            }

            if (cellSpaceCreatingLineStrings.size() == 3) { // 삼각형일 때
                LineString baseLS = cellSpaceCreatingLineStrings.get(0);
                Point p1 = baseLS.getPoints().get(0);
                Point p2 = baseLS.getPoints().get(1);
                if (left(p1, p2, snapPointsToCreatingCellSpace.get(0))) { // 반시계 방향이 되려면 직선의 왼쪽에 점이 와야한다.
                    baseLS.getPoints().clear();// 반시계 방향이 아니면 시작점과 끝점을 뒤집는다.
                    baseLS.getPoints().add(p2);
                    baseLS.getPoints().add(p1);

                    LineString secondLS = cellSpaceCreatingLineStrings.get(1);
                    p1 = secondLS.getPoints().get(0);
                    p2 = secondLS.getPoints().get(1);
                    secondLS.getPoints().clear();
                    secondLS.getPoints().add(p2);
                    secondLS.getPoints().add(p1);

                    LineString thirdLS = cellSpaceCreatingLineStrings.get(2);
                    p1 = thirdLS.getPoints().get(0);
                    p2 = thirdLS.getPoints().get(1);
                    thirdLS.getPoints().clear();
                    thirdLS.getPoints().add(p2);
                    thirdLS.getPoints().add(p1);
                    cellSpaceCreatingLineStrings.remove(2);
                    cellSpaceCreatingLineStrings.add(1, thirdLS);

                    System.out.println("left reverse");
                }
            }
        }

        previousMouseX = currentMouseX;
        previousMouseY = currentMouseY;

        repaint();
    }

    public void movePoint(Point p, int x1, int y1, int x2, int y2) {
        double panelX1 = (double) x1 / (floorPlanWidth * floorPlanScale);
        double panelY1 = (double) y1 / (floorPlanHeight * floorPlanScale);
        double panelX2 = (double) x2 / (floorPlanWidth * floorPlanScale);
        double panelY2 = (double) y2 / (floorPlanHeight * floorPlanScale);
        double offsetX = panelX2 - panelX1;
        double offsetY = panelY2 - panelY1;

        p.setPanelRatioX(p.getPanelRatioX() + offsetX);
        p.setPanelRatioY(p.getPanelRatioY() + offsetY);
        setPanelXYForCurrentScale(p);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        this.requestFocus();

        previousMouseX = e.getX();
        previousMouseY = e.getY();

        if (project.getEditState() == null)
            return;
        EditState currentEditState = project.getEditState();
        EditWorkState currentEditWorkState = project.getEditWorkState();
        if (e.getButton() == 1) {
            if (currentEditState == EditState.CREATE_STATE) {
                StateOnFloor stateOnFloor = project.getCurrentStateOnFloor();

                floorPlan = project.getCurrentFloorPlan();
                if (floorPlan == null)
                    return;
                floorPlanWidth = floorPlan.getWidth();
                floorPlanHeight = floorPlan.getHeight();
                floorPlanScale = project.getCurrentFloorPlanScale();

                State state = new State();
                Point point = new Point();

                if (e.getX() > floorPlanWidth * floorPlanScale)
                    return;
                if (e.getY() > floorPlanHeight * floorPlanScale)
                    return;

                point.setPanelX(e.getX());
                point.setPanelY(e.getY());
                setPanelRatioXY(point);

                state.setPosition(point);
                stateOnFloor.getStateMember().add(state);
            } else if (currentEditState == EditState.CREATE_TRANSITION) {
                selectedState = searchAdjacencyState(e);
                if (selectedState != null) {
                    if (stateEnd1 == null) {
                        stateEnd1 = selectedState;
                        selectedStateMap.put(stateEnd1, Color.YELLOW);
                        spaceLayerEnd1 = project.getCurrentSpaceLayer();

                        transitionPoints.add(selectedState.getPosition());
                        System.out.println("transitionEnd1");
                    } else if (stateEnd1 != null && stateEnd2 == null
                            && !stateEnd1.equals(selectedState)) {
                        stateEnd2 = selectedState;
                        selectedStateMap.put(stateEnd2, Color.YELLOW);
                        spaceLayerEnd2 = project.getCurrentSpaceLayer();

                        transitionPoints.add(selectedState.getPosition());
                        System.out.println("transitionEnd2");
                    }
                } else if (stateEnd1 != null && selectedState == null) {
                    Point point = new Point();

                    if (e.getX() > floorPlanWidth * floorPlanScale)
                        return;
                    if (e.getY() > floorPlanHeight * floorPlanScale)
                        return;

                    point.setPanelRatioX((double) e.getX() / (floorPlanWidth * floorPlanScale));
                    point.setPanelRatioY((double) e.getY() / (floorPlanHeight * floorPlanScale));

                    transitionPoints.add(point);
                }

                if (stateEnd1 != null && stateEnd2 != null) {
                    createTransition(transitionPoints, stateEnd1, stateEnd2);

            		transitionPoints.clear();
            		stateEnd1 = null;
            		stateEnd2 = null;
            		System.out.println("create transition");
                }
            } else if (currentEditState == EditState.CREATE_INTERLAYERCONNECTION) {
                System.out.println("mousepressed_createinterlayerconnection");
                if (currentKeyEvent == KeyEvent.KEY_RELEASED) {
                    if (currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND1) {
                        for (State state : statesEnd1) {
                            if (selectedStateMap.containsKey(state)) {
                                selectedStateMap.remove(state);
                            }
                        }
                        statesEnd1.clear();
                    } else if (currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND2) {
                        for (State state : statesEnd2) {
                            if (selectedStateMap.containsKey(state)) {
                                selectedStateMap.remove(state);
                            }
                        }
                        statesEnd2.clear();
                    }
                }
                if (currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND1
                        && spaceLayerEnd1 != project.getCurrentSpaceLayer()) {
                    for (State state : statesEnd1) {
                        if (selectedStateMap.containsKey(state)) {
                            selectedStateMap.remove(state);
                        }
                    }
                    statesEnd1.clear();
                } else if (currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND2
                        && spaceLayerEnd2 != project.getCurrentSpaceLayer()) {
                    for (State state : statesEnd2) {
                        if (selectedStateMap.containsKey(state)) {
                            selectedStateMap.remove(state);
                        }
                    }
                    statesEnd2.clear();
                }

                selectedState = searchAdjacencyState(e);
                if (selectedState != null) {
                    if (currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND1) {
                        statesEnd1.add(selectedState);
                        selectedStateMap.put(selectedState, Color.YELLOW);

                        spaceLayerEnd1 = project.getCurrentSpaceLayer();
                        System.out.println("statesEnd1");
                    } else if (!statesEnd1.contains(selectedState)
                            && currentEditWorkState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND2) {
                        statesEnd2.add(selectedState);
                        selectedStateMap.put(selectedState, Color.MAGENTA);

                        spaceLayerEnd2 = project.getCurrentSpaceLayer();
                        System.out.println("statesEnd2");
                    }
                }
            } else if (currentEditState == EditState.CREATE_CELLSPACE) {
                System.out.println("mousepressed_create cellspace");

                if (spaceLayerEnd1 != project.getCurrentSpaceLayer()) {
                    cellSpaceCreatingLineStrings.clear();
                    project.setEditWorkState(EditWorkState.CREATE_CELLSPACE_POINT1);
                    currentEditWorkState = project.getEditWorkState();
                    spaceLayerEnd1 = project.getCurrentSpaceLayer();
                }

                snapPointsToCreatingCellSpace.clear();
                searchSnapPointToCreatingCellSpace(e, snapPointsToCreatingCellSpace); // lineString의 점을 클릭하는지 확인
                if (snapPointsToCreatingCellSpace.size() == 0) {
                    Point point = searchSnapPointToCellSpace(e);
                    if (point == null) {
                        point = searchSnapPointToCellSpaceBoundary(e);
                    }
                    if (point == null) {
                        point = new Point();
                        point.setPanelX(e.getX());
                        point.setPanelY(e.getY());
                        setPanelRatioXY(point);
                    }

                    if (currentEditWorkState == EditWorkState.CREATE_CELLSPACE_POINT1) {
                        cellSpaceCreatingLineStrings.add(new LineString());
                        LineString newLineString = cellSpaceCreatingLineStrings
                                .get(cellSpaceCreatingLineStrings.size() - 1);
                        ArrayList<Point> newLSPoints = newLineString.getPoints();
                        newLSPoints.add(point);
                        newLineString.setPoints(newLSPoints);

                        project.setEditWorkState(EditWorkState.CREATE_CELLSPACE_POINT2);
                    } else if (currentEditWorkState == EditWorkState.CREATE_CELLSPACE_POINT2) {
                        LineString lineString = cellSpaceCreatingLineStrings
                                .get(cellSpaceCreatingLineStrings.size() - 1);
                        lineString.getPoints().add(point);

                        project.setEditWorkState(EditWorkState.CREATE_CELLSPACE_POINT3);
                    } else if (currentEditWorkState == EditWorkState.CREATE_CELLSPACE_POINT3
                            || currentEditWorkState == EditWorkState.CREATE_CELLSPACE_NEXTPOINT) {
                        LineString baseLS = null;
                        LineString newLS1 = new LineString();
                        LineString newLS2 = new LineString();

                        for (LineString ls : cellSpaceCreatingLineStrings) {
                            if (isAdjacencyPointToLineString(ls, point.getPanelX(),
                                    point.getPanelY())) {
                                baseLS = ls;
                                break;
                            }
                        }
                        if (baseLS != null) {
                            ArrayList<Point> ls1Points = newLS1.getPoints();
                            ArrayList<Point> ls2Points = newLS2.getPoints();
                            Point p1 = baseLS.getPoints().get(0);
                            Point p2 = baseLS.getPoints().get(1);
                            if (currentEditWorkState == EditWorkState.CREATE_CELLSPACE_POINT3) {
                                boolean leftSide = left(p1, p2, point);

                                // 원래는 if(!leftSide) { 여야 하지만
                                // swing의 xy좌표계는 왼쪽 위에서부터 (0, 0)으로 시작된다.
                                // 따라서 swing에서는 반시계 방향으로 보일 때, 실제로는 시계 방향이다.
                                // 하지만 gml로 출력시에는 swing에서 보이는 방향으로 출력되게 구현하였으므로
                                // 좌표 상에서는 시계방향으로 구성되도록 한다.
                                if (leftSide) { // 시계 방향이 되려면 직선의 오른쪽(반시계는 왼쪽)에 점이 와야한다.
                                    p1 = baseLS.getPoints().get(1); // 시계 방향이 아니면 시작점과 끝점을 뒤집는다.
                                    p2 = baseLS.getPoints().get(0);
                                    baseLS.getPoints().clear();
                                    baseLS.getPoints().add(p1);
                                    baseLS.getPoints().add(p2);

                                    System.out.println("left reverse_pressed");
                                }

                                ls1Points.add(p2.clone());
                                ls1Points.add(point);
                                ls2Points.add(point.clone());
                                ls2Points.add(p1.clone());
                            } else {
                                ls1Points.add(p1.clone());
                                ls1Points.add(point);
                                ls2Points.add(point.clone());
                                ls2Points.add(p2.clone());
                            }

                            newLS1.setPoints(ls1Points);
                            newLS2.setPoints(ls2Points);
                            cellSpaceCreatingLineStrings.add(
                                    cellSpaceCreatingLineStrings.indexOf(baseLS) + 1, newLS1);
                            cellSpaceCreatingLineStrings.add(
                                    cellSpaceCreatingLineStrings.indexOf(baseLS) + 2, newLS2);
                            if (currentEditWorkState == EditWorkState.CREATE_CELLSPACE_NEXTPOINT) {
                                cellSpaceCreatingLineStrings.remove(baseLS);
                            }
                            project.setEditWorkState(EditWorkState.CREATE_CELLSPACE_NEXTPOINT);
                        }
                    }
                }
            } else if (currentEditState == EditState.CREATE_CELLSPACEBOUNDARY_AS_DOOR) {
                // Point snapPoint = searchSnapPointToCellSpaceBoundary(e);
            	Map<String, Object> map = searchSnapPointToCellSpace(e, null);
                Point snapPoint = (Point) map.get("Point");
            	LineString baseDoorLine2 = (LineString) map.get("BaseLine");
                
                if (snapPoint != null) {
                    double doorHeight = project.getCurrentCellSpaceBoundaryOnFloor()
                            .getFloorProperty().getDoorHeight();
                    snapPoint.setZ(doorHeight);
                    if (doorPointList.size() == 0) {
                        doorPointList.add(snapPoint);
                        baseDoorLine = baseDoorLine2;
                    } else if (doorPointList.size() == 1 && baseDoorLine.equals(baseDoorLine2)) {
                    	doorPointList.add(snapPoint);
                    }
                    
                    if (doorPointList.size() == 2) {
                        createCellSpaceBoundaryAsDoor(map);

                        doorPointList.clear();
                        baseDoorLine = null;
                        project.setEditState(EditState.NONE);
                        mainFrame.setLabel_CurrentEditState("");
                    }
                }
            } else if (currentEditState == EditState.NONE
                    || currentEditState == EditState.SELECT_STATE
                    || currentEditState == EditState.SELECT_CELLSPACE
                    || currentEditState == EditState.SELECT_TRANSITION
                    || currentEditState == EditState.SELECT_CELLSPACEBOUNDARY) {
                boolean isSelected = false;
                selectedState = null;
                selectedCellSpace = null;
                selectedTransition = null;
                selectedCellSpaceBoundary = null;
                if (currentKeyEvent == KeyEvent.KEY_RELEASED) {
                    selectedStateMap.clear();
                    selectedCellSpaceMap.clear();
                    selectedTransitionMap.clear();
                    selectedCellSpaceBoundaryMap.clear();
                }

                selectedState = searchAdjacencyState(e);
                if (selectedState != null) {
                    if (currentEditState != EditState.SELECT_STATE) {
                        selectedStateMap.clear();
                        selectedCellSpaceMap.clear();
                    }
                    selectedStateMap.put(selectedState, Color.YELLOW);
                    if (selectedState.getDuality() != null) {
                        selectedCellSpaceMap.put(selectedState.getDuality(), Color.CYAN);
                    }

                    project.setEditState(EditState.SELECT_STATE);
                    isSelected = true;
                    
                    String selectedStateIDs = "";                    
                    for(State selected : selectedStateMap.keySet()) {
                        selectedStateIDs += selected.getGmlID() + ", ";
                        //System.out.println("State : " + selected.getGmlID() + ", " + selected.getPosition().getPanelRatioX() + " " + (1 - selected.getPosition().getPanelRatioY()));
                    }
                    selectedStateIDs = selectedStateIDs.substring(0, selectedStateIDs.length() - 2);
                    if(selectedStateMap.size() > 1) {
                    	mainFrame.setLabel_CurrentEditState("Selected State : " + selectedStateIDs);
                    } else if(selectedStateMap.size() == 1) {
                    	String dualityID = null;
                    	if(selectedState.getDuality() != null) {
                    		dualityID = selectedState.getDuality().getGmlID();
                    	}
                    	mainFrame.setLabel_CurrentEditState("Selected State : " + selectedStateIDs + " duality : " + dualityID);
                    }
                }

                if (!isSelected) {
                    selectedTransition = searchAdjacencyTransition(e);
                    if (selectedTransition != null) {
                        if (currentEditState != EditState.SELECT_TRANSITION) {
                            selectedTransitionMap.clear();
                            selectedCellSpaceBoundaryMap.clear();
                        }
                        selectedTransitionMap.put(selectedTransition, Color.YELLOW);
                        if (selectedTransition.getDuality() != null) {
                            selectedCellSpaceBoundaryMap.put(selectedTransition.getDuality(),
                                    Color.CYAN);
                        }

                        project.setEditState(EditState.SELECT_TRANSITION);
                        isSelected = true;
                        
                        String selectedTransitionIDs = "";
                        for(Transition selected : selectedTransitionMap.keySet()) {
                            selectedTransitionIDs += selected.getGmlID() + ", ";
                        }
                        selectedTransitionIDs = selectedTransitionIDs.substring(0, selectedTransitionIDs.length() - 2);
                        mainFrame.setLabel_CurrentEditState("Selected Transition : " + selectedTransitionIDs);
                    }
                }

                if (!isSelected) {
                    selectedCellSpaceBoundary = searchAdjacencyCellSpaceBoundary(e);
                    if (selectedCellSpaceBoundary != null) {
                        if (currentEditState != EditState.SELECT_CELLSPACEBOUNDARY) {
                            selectedCellSpaceBoundaryMap.clear();
                            selectedTransitionMap.clear();
                        }
                        selectedCellSpaceBoundaryMap.put(selectedCellSpaceBoundary, Color.YELLOW);
                        if (selectedCellSpaceBoundary.getDuality() != null) {
                            selectedTransitionMap.put(selectedCellSpaceBoundary.getDuality(),
                                    Color.CYAN);
                        }

                        project.setEditState(EditState.SELECT_CELLSPACEBOUNDARY);
                        isSelected = true;
                        
                        String selectedCellSpaceBoundaryIDs = "";
                        for(CellSpaceBoundary selected : selectedCellSpaceBoundaryMap.keySet()) {
                            selectedCellSpaceBoundaryIDs += selected.getGmlID() + ", ";
                        }
                        selectedCellSpaceBoundaryIDs = selectedCellSpaceBoundaryIDs.substring(0, selectedCellSpaceBoundaryIDs.length() - 2);
                        mainFrame.setLabel_CurrentEditState("Selected CellSpace : " + selectedCellSpaceBoundaryIDs);
                    }
                }

                if (!isSelected) {
                    selectedCellSpace = searchPointInCellSpace(e);
                    if (selectedCellSpace != null) {
                        if (currentEditState != EditState.SELECT_CELLSPACE) {
                            selectedStateMap.clear();
                            selectedCellSpaceMap.clear();
                        }
                        selectedCellSpaceMap.put(selectedCellSpace, Color.YELLOW);
                        if (selectedCellSpace.getDuality() != null) {
                            selectedStateMap.put(selectedCellSpace.getDuality(), Color.CYAN);
                        }

                        project.setEditState(EditState.SELECT_CELLSPACE);
                        isSelected = true;
                        
                        String selectedCellSpaceIDs = "";
                        for(CellSpace selected : selectedCellSpaceMap.keySet()) {
                            selectedCellSpaceIDs += selected.getGmlID() + ", ";
                        }
                        selectedCellSpaceIDs = selectedCellSpaceIDs.substring(0, selectedCellSpaceIDs.length() - 2);
                        mainFrame.setLabel_CurrentEditState("Selected CellSpace : " + selectedCellSpaceIDs);
                    }
                }

                if (!isSelected) {
                    project.setEditState(EditState.NONE);
                    mainFrame.setLabel_CurrentEditState("");
                    selectedStateMap.clear();
                    selectedCellSpaceMap.clear();
                    selectedTransitionMap.clear();
                    selectedCellSpaceBoundaryMap.clear();
                }
            } else if (currentEditState == EditState.CREATE_STATE_DUALITY) {
                selectedCellSpace = searchPointInCellSpace(e);

                if (selectedCellSpace != null) {
                    if (selectedState.getDuality() != null) {
                        selectedCellSpaceMap.remove(selectedState.getDuality());
                        selectedState.getDuality().setDuality(null);
                    }
                    selectedState.setDuality(selectedCellSpace);
                    selectedCellSpace.setDuality(selectedState);
                    selectedCellSpaceMap.put(selectedCellSpace, Color.CYAN);
                    System.out.println("create state duality");
                }

                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            } else if (currentEditState == EditState.CREATE_CELLSPACE_DUALITY) {
                selectedState = searchAdjacencyState(e);

                if (selectedState != null) {
                    if (selectedCellSpace.getDuality() != null) {
                        selectedStateMap.remove(selectedCellSpace.getDuality());
                        selectedCellSpace.getDuality().setDuality(null);
                    }
                    selectedCellSpace.setDuality(selectedState);
                    selectedState.setDuality(selectedCellSpace);
                    selectedStateMap.put(selectedState, Color.CYAN);
                    System.out.println("create cellspace duality");
                }
                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            } else if (currentEditState == EditState.CREATE_TRANSITION_DUALITY) {
                selectedCellSpaceBoundary = searchAdjacencyCellSpaceBoundary(e);

                if (selectedCellSpaceBoundary != null) {
                    if (selectedTransition.getDuality() != null) {
                        selectedCellSpaceBoundaryMap.remove(selectedTransition.getDuality());
                        selectedTransition.getDuality().setDuality(null);
                    }
                    selectedTransition.setDuality(selectedCellSpaceBoundary);
                    selectedCellSpaceBoundary.setDuality(selectedTransition);
                    selectedCellSpaceBoundaryMap.put(selectedCellSpaceBoundary, Color.CYAN);
                    System.out.println("create transition duality");
                }
                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            } else if (currentEditState == EditState.CREATE_CELLSPACEBOUNDARY_DUALITY) {
                selectedTransition = searchAdjacencyTransition(e);

                if (selectedTransition != null) {
                    if (selectedCellSpaceBoundary.getDuality() != null) {
                        selectedTransitionMap.remove(selectedCellSpaceBoundary.getDuality());
                        selectedCellSpaceBoundary.getDuality().setDuality(null);
                    }
                    selectedCellSpaceBoundary.setDuality(selectedTransition);
                    selectedTransition.setDuality(selectedCellSpaceBoundary);
                    selectedTransitionMap.put(selectedTransition, Color.CYAN);
                    System.out.println("create cellspaceboundary duality");
                }
                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            }
        } else if (e.getButton() == 3) { // 우클릭
            boolean selected = false;
            selectedState = searchAdjacencyState(e);
            if (selectedState != null) {
                if (!selectedStateMap.containsKey(selectedState)) {
                    selectedStateMap.clear();
                    selectedCellSpaceMap.clear();
                }
                selectedStateMap.put(selectedState, Color.YELLOW);
                if (selectedState.getDuality() != null) {
                    selectedCellSpaceMap.put(selectedState.getDuality(), Color.CYAN);
                }

                project.setEditState(EditState.SELECT_STATE);

                System.out.println("select state");
                selected = true;
                getPopupMenu_State().show(this, e.getX(), e.getY());
            }

            if (!selected) {
                selectedTransition = searchAdjacencyTransition(e);
                if (selectedTransition != null) {
                    if (!selectedTransitionMap.containsKey(selectedTransition)) {
                        selectedTransitionMap.clear();
                        selectedCellSpaceBoundaryMap.clear();
                    }
                    selectedTransitionMap.put(selectedTransition, Color.YELLOW);
                    if (selectedTransition.getDuality() != null) {
                        selectedCellSpaceBoundaryMap.put(selectedTransition.getDuality(),
                                Color.CYAN);
                    }

                    project.setEditState(EditState.SELECT_TRANSITION);

                    System.out.println("select transition");
                    selected = true;
                    getPopupMenu_Transition().show(this, e.getX(), e.getY());
                }
            }

            if (!selected) {
                selectedCellSpaceBoundary = searchAdjacencyCellSpaceBoundary(e);
                if (selectedCellSpaceBoundary != null) {
                    if (!selectedCellSpaceBoundaryMap.containsKey(selectedCellSpaceBoundary)) {
                        selectedCellSpaceBoundaryMap.clear();
                        selectedTransitionMap.clear();
                    }
                    selectedCellSpaceBoundaryMap.put(selectedCellSpaceBoundary, Color.YELLOW);
                    if (selectedCellSpaceBoundary.getDuality() != null) {
                        selectedTransitionMap.put(selectedCellSpaceBoundary.getDuality(),
                                Color.CYAN);
                    }

                    project.setEditState(EditState.SELECT_CELLSPACEBOUNDARY);

                    System.out.println("select cellspaceboundary");
                    selected = true;
                    getPopupMenu_CellSpaceBoundary().show(this, e.getX(), e.getY());
                }
            }

            if (!selected) {
                selectedCellSpace = searchPointInCellSpace(e);
                if (selectedCellSpace != null) {
                    if (!selectedCellSpaceMap.containsKey(selectedCellSpace)) {
                        selectedCellSpaceMap.clear();
                        selectedStateMap.clear();
                    }
                    selectedCellSpaceMap.put(selectedCellSpace, Color.YELLOW);
                    if (selectedCellSpace.getDuality() != null) {
                        selectedStateMap.put(selectedCellSpace.getDuality(), Color.CYAN);
                    }

                    project.setEditState(EditState.SELECT_CELLSPACE);

                    System.out.println("select cellspace");
                    selected = true;
                    getPopupMenu_CellSpace().show(this, e.getX(), e.getY());
                }
            }

            if (!selected) {
                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
                selectedStateMap.clear();
                selectedCellSpaceMap.clear();
                selectedTransitionMap.clear();
                selectedCellSpaceBoundaryMap.clear();
            }
        }

        repaint();
    }

	private void createTransition(ArrayList<Point> transitionPoints, State stateEnd1, State stateEnd2) {
		TransitionOnFloor transitionOnFloor = project.getCurrentTransitionOnFloor();
		Transition transition = new Transition();
		LineString path = new LineString();
		ArrayList<Point> pathPoints = path.getPoints();

		pathPoints.addAll(transitionPoints);
		path.setPoints(pathPoints);

		transition.setStates(new State[] { stateEnd1, stateEnd2 });
		transition.setPath(path);
		
		stateEnd1.getTransitionReference().add(transition);
		stateEnd2.getTransitionReference().add(transition);

		transitionOnFloor.getTransitionMember().add(transition);
	}

    // CellSpace, CellSpaceBoundary 생성 순서
    // 일단 처음 CellSpace는 그냥 만든다.
    // 처음 2개의 점을 찍을 때는
    // a) 이미 생성된 CellSpace들의 점
    // b) CellSpace의 점들을 잇는 LineString 위의 점에 가깝게 만들어 준다.

    public CellSpace createCellSpace(ArrayList<LineString> lineStringElements) {
        HashMap<LineString, ArrayList<LineString>> xLink2DMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getxLink2DMap();

        HashMap<LineString, ArrayList<CellSpaceBoundary>> lineStringOfAdjacencyBoundaryMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getLineStringOfAdjacencyBoundaryMap();
        HashMap<CellSpaceBoundary, ArrayList<CellSpace>> boundaryOfReferenceCellSpaceMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getBoundaryOfReferenceCellSpaceMap();

        ArrayList<CellSpace> cellSpaceMember = project.getCurrentCellSpaceOnFloor()
                .getCellSpaceMember();

        // create CellSpace
        CellSpace cellSpace = new CellSpace();

        // CounterClockwised polygon check
        ArrayList<Point> checkPoints = new ArrayList<Point>();
        for (LineString ls : lineStringElements) {
            checkPoints.add(ls.getPoints().get(0).clone());
        }
        checkPoints.add(checkPoints.get(0).clone());

        LinearRing checkRing = new LinearRing();
        checkRing.setPoints(checkPoints);
        com.vividsolutions.jts.geom.LineString jtsLine = JTSUtil.convertJTSLineString(checkRing);
        if (JTSUtil.Orientation2D_Polygon(jtsLine.getNumPoints(), jtsLine.getCoordinateSequence()) > 0) {
        	ArrayList<LineString> temp = new ArrayList<LineString>();
        	for (int i = checkPoints.size() - 1; i >= 0; i--) {
        		LineString tempLine = new LineString();
        		ArrayList<Point> tempPoints = tempLine.getPoints();
        		tempPoints.add(checkPoints.get(i).clone());
        		if (i == 0) {
        			tempPoints.add(checkPoints.get(checkPoints.size() - 1).clone());
        		} else {
        			tempPoints.add(checkPoints.get(i - 1).clone());
        		}
        		temp.add(tempLine);
        	}
        	lineStringElements = temp;
        	/*
        	int last = lineStringElements.size() - 1;
        	for (int i = last; i >= 0; i--) {
        		LineString line = lineStringElements.get(i);
        		Point p = line.getPoints().get(0);
        		line.getPoints().add(p);
        		line.getPoints().remove(0);
        		
        		temp.add(lineStringElements.get(i));
        	}
        	lineStringElements = temp;
        	*/
        	System.out.println("reversed");
        }
        //
        for (int i = 0; i < lineStringElements.size(); i++) {
            LineString ls = lineStringElements.get(i);
            // Point p1 = ls.getPoints().get(0);
            // Point p2 = ls.getPoints().get(1);
            // exteriorPoints.add(p1.clone());

            // CellSpace를 생성할 때 2개의 점을 가지고 모든 벽에 대한 CellSpaceBoundary를 생성하던 것에서
            // 다른 CellSpace와 붙어있게 될 때만 해당 기하를 가지는 CellSpaceBoundary를 생성하도록 변경함
            for (CellSpace otherCellSpace : cellSpaceMember) {
                for (LineString otherLS : otherCellSpace.getLineStringElements()) {
                    if (GeometryUtil.isContainsLineString(ls, otherLS)
                            || GeometryUtil.isContainsLineString(otherLS, ls)
                            || GeometryUtil.isOverlapsLineString(ls, otherLS)) { // ls가 otherLS를 포함하면 otherLS의 기하를 가지는 boundary생성
                        System.out.println("overlaped boundary checked");
                        if (!lineStringOfAdjacencyBoundaryMap.containsKey(otherLS)) { // 해당 lineString을 가지는 CellSpaceBoundary가 있을 경우의 수는
                        	createCellSpaceBoundary(ls, otherLS, cellSpace, otherCellSpace); 
                        } else { // 이미 boundary가 존재할 경우에만 else로 넘어온다.
                            // 벽의 일부씩만 겹쳤을 때 intersection으로 boundary 생성해주는 부분 추가해야한다.
                            // 지금은 새로 만들어지는 벽이 기존에 있는 문을 포함할 경우만 해놓음.
                            ArrayList<CellSpaceBoundary> adjacencyBoundaryList = lineStringOfAdjacencyBoundaryMap
                                    .get(otherLS);
                            
                            CellSpaceBoundary targetBoundary = null;
                            for (CellSpaceBoundary existBoundary : adjacencyBoundaryList) {
                            	LineString existLS = existBoundary.getGeometry2D();
                            	if (GeometryUtil.isContainsLineString(ls, existLS)
                                        || GeometryUtil.isContainsLineString(existLS, ls)
                                        || GeometryUtil.isOverlapsLineString(ls, existLS)) {
                            		targetBoundary = existBoundary;
                            	}
                            }
                            
                            if (targetBoundary != null) {
                            	// Cell의 Line에 Boundary는 있지만 Cell과 붙어있지 않은 경우
                            	// 외부로 향하는 문으로 만들어놨는데 Cell이 붙는 경우
                            	// 문이 1개인 경우는 간단하지만 문이 한쪽 벽에 여러개 있을 때
                            	// 여기에 추가로 Cell이 생성되면 여러개의 문에 대한 Line 분할 필요
                            	System.out.println("targetBoundary is not null");
                            } else {
                            	createCellSpaceBoundary(ls, otherLS, cellSpace, otherCellSpace);
                            }
                            
                           
                        }
                    }
                }
            }

        }
        cellSpace.setLineStringElements((ArrayList<LineString>) lineStringElements.clone());
        ArrayList<Point> exteriorPoints = new ArrayList<Point>();
        for (LineString ls : cellSpace.getLineStringElements()) {
            exteriorPoints.add(ls.getPoints().get(0).clone());
        }
        exteriorPoints.add(exteriorPoints.get(0).clone());

        LinearRing exteriorRing = new LinearRing();
        exteriorRing.setPoints(exteriorPoints);
        Polygon geometry2D = new Polygon();
        geometry2D.setExteriorRing(exteriorRing);
        cellSpace.setGeometry2D(geometry2D);
        project.getCurrentCellSpaceOnFloor().getCellSpaceMember().add(cellSpace);

        // CellSpace를 생성 후 duality를 가지는 State 자동으로 생성
        StateOnFloor stateOnFloor = project.getCurrentStateOnFloor();
        State dualityState = new State();
        Point point = GeometryUtil.getCentroidPointOnPolygon(geometry2D);
        setPanelRatioXY(point);
        dualityState.setPosition(point);
        dualityState.setDuality(cellSpace);
        stateOnFloor.getStateMember().add(dualityState);
        cellSpace.setDuality(dualityState);
        
        // 변경 전
        // search adjacency boundary
        /*
         * for(CellSpaceBoundary boundary : cellSpace.getPartialBoundedBy()) { LineString ls = boundary.getGeometry2D(); if(ls.getxLinkGeometry() !=
         * null) continue;
         * 
         * for(CellSpace c : project.getCurrentCellSpaceOnFloor().getCellSpaceMember()) { if(c.equals(cellSpace)) continue;
         * 
         * for(CellSpaceBoundary otherBoundary : c.getPartialBoundedBy()) { LineString otherls = otherBoundary.getGeometry2D(); if(otherls == null)
         * continue; // 3차원기하만 있을 경우 if(otherls.getxLinkGeometry() != null) continue;
         * 
         * if(colLinear(otherls, ls)) { // boundary의 기하가 서로 평행하게 인접한 것을 찾는다. if(!adjacencyBoundaryMap.containsKey(otherBoundary)) {
         * adjacencyBoundaryMap.put(otherBoundary, new ArrayList<CellSpaceBoundary>()); } if(!adjacencyBoundaryMap.containsKey(boundary)) {
         * adjacencyBoundaryMap.put(boundary, new ArrayList<CellSpaceBoundary>()); } // 인접한 양쪽에 대한 정보를 map에 저장한다.
         * adjacencyBoundaryMap.get(otherBoundary).add(boundary); adjacencyBoundaryMap.get(boundary).add(otherBoundary); }
         * 
         * } } }
         */

        return cellSpace;
    }
    
    public CellSpaceBoundary createCellSpaceBoundary(LineString geometry2D) {
    	CellSpaceBoundary newBoundary = new CellSpaceBoundary(); // 1. 다른 방과 붙어 있어 벽에 대한 boundary가 있는 경우. 이 때는 여기 조건에 포함되지 않음
        
        ArrayList<Point> newPoints = geometry2D.getPoints();
        for (Point p : newPoints) {
        	setPanelRatioXY(p);
        }
        geometry2D.setPoints(newPoints);
        
        geometry2D.setIsReversed(false);
        newBoundary.setGeometry2D(geometry2D);
        newBoundary.setBoundaryType(BoundaryType.CellSpaceBoundary);
        
        return newBoundary;
    }
    
    public void createCellSpaceBoundary(LineString ls, LineString otherLS, CellSpace c1, CellSpace c2) {
    	HashMap<LineString, ArrayList<CellSpaceBoundary>> lineStringOfAdjacencyBoundaryMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getLineStringOfAdjacencyBoundaryMap();
        HashMap<CellSpaceBoundary, ArrayList<CellSpace>> boundaryOfReferenceCellSpaceMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getBoundaryOfReferenceCellSpaceMap();
                
        // geometry2d of boundary will be a intersection between ls and otherLS.
        LineString intersection = GeometryUtil.getIntersectionLineString(
                ls, otherLS);
        if (intersection == null) {
        	System.out.println("Intersection is null");
        	return;
        }
        CellSpaceBoundary newBoundary = createCellSpaceBoundary(intersection.clone());
        String c1Usage = c1.getDescription("Usage");
        String c2Usage = c2.getDescription("Usage");
        if ((c1Usage != null && c2Usage != null) && (c1Usage.equals("Door") || c2Usage.equals("Door"))) {
        	newBoundary.setBoundaryType(BoundaryType.Door);
        }
        
        c1.getPartialBoundedBy().add(newBoundary);
        c2.getPartialBoundedBy().add(newBoundary);

        if (!lineStringOfAdjacencyBoundaryMap.containsKey(ls)) {
            lineStringOfAdjacencyBoundaryMap.put(ls,
                    new ArrayList<CellSpaceBoundary>());
        }
        if (!lineStringOfAdjacencyBoundaryMap.containsKey(otherLS)) {
            lineStringOfAdjacencyBoundaryMap.put(otherLS,
                    new ArrayList<CellSpaceBoundary>());
        }
        lineStringOfAdjacencyBoundaryMap.get(ls).add(newBoundary);
        lineStringOfAdjacencyBoundaryMap.get(otherLS).add(newBoundary);

        if (!boundaryOfReferenceCellSpaceMap.containsKey(newBoundary)) {
            boundaryOfReferenceCellSpaceMap.put(newBoundary,
                    new ArrayList<CellSpace>());
        }
        boundaryOfReferenceCellSpaceMap.get(newBoundary).add(c1);
        boundaryOfReferenceCellSpaceMap.get(newBoundary).add(c2);

        project.getCurrentCellSpaceBoundaryOnFloor()
                .getCellSpaceBoundaryMember().add(newBoundary);
    }
    
    public void createCellSpaceBoundaryAsDoor(Map<String, Object> map) {
    	HashMap<LineString, ArrayList<CellSpaceBoundary>> lineStringOfAdjacencyBoundaryMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getLineStringOfAdjacencyBoundaryMap();
        HashMap<CellSpaceBoundary, ArrayList<CellSpace>> boundaryOfReferenceCellSpaceMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getBoundaryOfReferenceCellSpaceMap();
        
        LineString doorLineString = new LineString();
        doorLineString.setPoints((ArrayList<Point>) doorPointList.clone());
        
        CellSpace baseCellSpace = (CellSpace) map.get("CellSpace");
        LineString baseLine = (LineString) map.get("BaseLine");
        
        if (baseCellSpace == null) {
        	System.out.println("Base CellSpace is null");
        }
        if (baseLine == null) {
        	System.out.println("Base Door Line is null");
        }
        
        ArrayList<CellSpaceBoundary> adjacencyBoundaryList = lineStringOfAdjacencyBoundaryMap.get(baseLine);
        
        if (!lineStringOfAdjacencyBoundaryMap.containsKey(baseLine) || adjacencyBoundaryList.size() == 0) {
        	CellSpaceBoundary doorBoundary = createCellSpaceBoundary(doorLineString);
        	//doorBoundary.setBoundaryType(BoundaryType.Door);
        	baseCellSpace.getPartialBoundedBy().add(doorBoundary);

            if (!lineStringOfAdjacencyBoundaryMap.containsKey(baseLine)) {
                lineStringOfAdjacencyBoundaryMap.put(baseLine,
                        new ArrayList<CellSpaceBoundary>());
            }
            lineStringOfAdjacencyBoundaryMap.get(baseLine).add(doorBoundary);
            if (!boundaryOfReferenceCellSpaceMap.containsKey(doorBoundary)) {
                boundaryOfReferenceCellSpaceMap.put(doorBoundary,
                        new ArrayList<CellSpace>());
            }
            boundaryOfReferenceCellSpaceMap.get(doorBoundary).add(baseCellSpace);
            project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().add(doorBoundary);
        } else {
        	CellSpaceBoundary deleted = null;
        	ArrayList<CellSpaceBoundary> splitedBoundary = null;
        	for (CellSpaceBoundary adjacencyBoundary : adjacencyBoundaryList) {
        		LineString adjacencyLS = adjacencyBoundary.getGeometry2D();
        		
        		if (GeometryUtil.isContainsLineString(adjacencyLS, doorLineString)
                        && adjacencyBoundary.getBoundaryType() != BoundaryType.Door) { // 기존 boundary에 door가 생긴다면
        			splitedBoundary = splitCellSpaceBoundary(adjacencyBoundary, doorLineString); // boundary 쪼갠다.
        			
        			deleted = adjacencyBoundary;
        		}
        	}
        	
        	if (deleted != null) {
	        	int idx = adjacencyBoundaryList.indexOf(deleted);
	        	adjacencyBoundaryList.remove(deleted);
	        	adjacencyBoundaryList.addAll(idx, splitedBoundary);
	        	baseCellSpace.getPartialBoundedBy().remove(deleted);
	        	baseCellSpace.getPartialBoundedBy().addAll(splitedBoundary);        	
	        	
	        	ArrayList<CellSpace> referenceCellSpaceList = boundaryOfReferenceCellSpaceMap.get(deleted);
	        	CellSpace otherReference = null;
	        	for (CellSpace otherCellSpace : referenceCellSpaceList) {
	        		if (!baseCellSpace.equals(otherCellSpace)) {
	        			ArrayList<LineString> otherLSElements = otherCellSpace.getLineStringElements();
	        			boolean check = false;
	        			for (LineString otherLS : otherLSElements) {
	        				if (lineStringOfAdjacencyBoundaryMap.containsKey(otherLS)) {
	                            if (lineStringOfAdjacencyBoundaryMap.get(otherLS).contains(deleted)) {
	                                lineStringOfAdjacencyBoundaryMap.get(otherLS).remove(deleted);
	                                lineStringOfAdjacencyBoundaryMap.get(otherLS).addAll(splitedBoundary);
	                                otherCellSpace.getPartialBoundedBy().remove(deleted);
	                                otherCellSpace.getPartialBoundedBy().addAll(splitedBoundary);
	                                
	                                otherReference = otherCellSpace;
	                                check = true;
	                                break;
	                            }
	                        }
	        			}
	        			
	        			if (check) {
	        				break;
	        			}
	        		}
	        	}
	        	
	        	//
	        	boundaryOfReferenceCellSpaceMap.remove(deleted);
	        	for (CellSpaceBoundary splited : splitedBoundary) {
	        		boundaryOfReferenceCellSpaceMap.put(splited, new ArrayList<CellSpace>());
	        		boundaryOfReferenceCellSpaceMap.get(splited).add(baseCellSpace);
	        		boundaryOfReferenceCellSpaceMap.get(splited).add(otherReference);
	        	}
	        	project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().remove(deleted);
	        	project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().addAll(splitedBoundary);
        	} else {
        		CellSpaceBoundary doorBoundary = createCellSpaceBoundary(doorLineString);
            	doorBoundary.setBoundaryType(BoundaryType.Door);
            	baseCellSpace.getPartialBoundedBy().add(doorBoundary);

                if (!lineStringOfAdjacencyBoundaryMap.containsKey(baseLine)) {
                    lineStringOfAdjacencyBoundaryMap.put(baseLine,
                            new ArrayList<CellSpaceBoundary>());
                }
                lineStringOfAdjacencyBoundaryMap.get(baseLine).add(doorBoundary);
                if (!boundaryOfReferenceCellSpaceMap.containsKey(doorBoundary)) {
                    boundaryOfReferenceCellSpaceMap.put(doorBoundary,
                            new ArrayList<CellSpace>());
                }
                boundaryOfReferenceCellSpaceMap.get(doorBoundary).add(baseCellSpace);
                project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().add(doorBoundary);
        	}
        }
    }

    public void createCellSpaceBoundaryAsDoor(LineString baseDoorLine) {
        HashMap<LineString, ArrayList<LineString>> xLink2DMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getxLink2DMap();

        //
        HashMap<LineString, ArrayList<CellSpaceBoundary>> lineStringOfAdjacencyBoundaryMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getLineStringOfAdjacencyBoundaryMap();
        HashMap<CellSpaceBoundary, ArrayList<CellSpace>> boundaryOfReferenceCellSpaceMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getBoundaryOfReferenceCellSpaceMap();

        // 변경 후
        LineString doorLineString = new LineString();
        doorLineString.setPoints((ArrayList<Point>) doorPointList.clone());

        ArrayList<CellSpace> cellSpaceMember = project.getCurrentCellSpaceOnFloor()
                .getCellSpaceMember();
        for (CellSpace cellSpace : cellSpaceMember) {
            ArrayList<LineString> lineStringElements = cellSpace.getLineStringElements();
            for (int i = 0; i < lineStringElements.size(); i++) {
                LineString ls = lineStringElements.get(i);
                /*
                if (GeometryUtil.isContainsLineString(ls, doorLineString)
                        || GeometryUtil.isContainsLineString(doorLineString, ls)
                        || GeometryUtil.isOverlapsLineString(ls, doorLineString)) {
                        */
                if (ls.equals(baseDoorLine)) {
                    // 일단 기하를 나눈다.

                    ArrayList<LineString> splitedLS = GeometryUtil.splitLineString(ls,
                            doorLineString); // lineString 분할
                    for(LineString splited : splitedLS) {
                            for(Point p : splited.getPoints()) {
                                    setPanelRatioXY(p);
                            }
                    }
                    
                    LineString doorInThisCellSpace = null;
                    int insertCount = 0;
                    for (int j = 0; j < splitedLS.size(); j++) {
                        LineString split = splitedLS.get(j);
                        ArrayList<Point> splitPoints = split.getPoints();

                        /*
                         * if(j == 0) { int tempi; if(i == 0) tempi = lineStringElements.size() - 1; else tempi = i - 1;
                         * 
                         * lineStringElements.get(tempi).getPoints().get(1).setZ(doorPointList.get(0).getZ()); } else if(j == splitedLS.size() - 1) {
                         * lineStringElements.get((i + 2) % lineStringElements.size()).getPoints().get(0).setZ(doorPointList.get(0).getZ()); }
                         */

                        boolean isEquals = true;
                        for (Point doorPoint : doorPointList) {
                        	boolean check = false;
                        	for (Point splitPoint : splitPoints) {
                        		if (doorPoint.equalsPanelRatioXY(splitPoint)) {
                        			check = true;
                        			break;
                        		}
                        	}
                        	
                        	if (!check) {
                        		isEquals = false;
                        		break;
                        	}
                        }
                        if (isEquals) {
                            doorInThisCellSpace = split;
                        }
                        lineStringElements.add(i + j + 1, split);
                    }
                    i = i + splitedLS.size() + 1;

                    // adjacencyBoundary가 없을 경우 -> 붙은 벽이 없다 -> 그냥 문 boundary만 생성해서 추가
                    if (!lineStringOfAdjacencyBoundaryMap.containsKey(ls)) {
                        CellSpaceBoundary newBoundary = new CellSpaceBoundary();
                        newBoundary.setBoundaryType(BoundaryType.Door);
                        newBoundary.setGeometry2D(doorLineString);

                        cellSpace.getPartialBoundedBy().add(newBoundary);

                        if (!lineStringOfAdjacencyBoundaryMap.containsKey(doorInThisCellSpace)) {
                            lineStringOfAdjacencyBoundaryMap.put(doorInThisCellSpace,
                                    new ArrayList<CellSpaceBoundary>());
                        }
                        lineStringOfAdjacencyBoundaryMap.get(doorInThisCellSpace).add(newBoundary);
                        if (!boundaryOfReferenceCellSpaceMap.containsKey(newBoundary)) {
                            boundaryOfReferenceCellSpaceMap.put(newBoundary,
                                    new ArrayList<CellSpace>());
                        }
                        boundaryOfReferenceCellSpaceMap.get(newBoundary).add(cellSpace);
                        project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember()
                                .add(newBoundary);
                    } else {
                        // adjacencyBoundary가 있을 경우 -> 문이 boundary에 포함이 되는지 확인 -> 포함되면 boundary의 기하를 분할
                        // -> 분할하고 난 뒤에 다른 cell의 list에 모두 추가 -> 분할한 기하와 분할한 boundary 인접 비교해서 재설정
                        boolean isSplitedByDoor = false;
                        ArrayList<CellSpaceBoundary> adjacencyBoundaryList = lineStringOfAdjacencyBoundaryMap
                                .get(ls);
                        for (int j = 0; j < adjacencyBoundaryList.size(); j++) {
                            CellSpaceBoundary adjacencyBoundary = adjacencyBoundaryList.get(j);
                            LineString boundaryLS = adjacencyBoundary.getGeometry2D();

                            if (GeometryUtil.isContainsLineString(boundaryLS, doorLineString)
                                    && adjacencyBoundary.getBoundaryType() != BoundaryType.Door) { // 기존 boundary에 door가 생긴다면
                                adjacencyBoundaryList.remove(j); // linestringOfadjacencyboundarymap.get(ls).remove();
                                cellSpace.getPartialBoundedBy().remove(adjacencyBoundary);
                                boundaryOfReferenceCellSpaceMap.remove(adjacencyBoundary);
                                project.getCurrentCellSpaceBoundaryOnFloor()
                                        .getCellSpaceBoundaryMember().remove(adjacencyBoundary);

                                ArrayList<CellSpaceBoundary> splitedBoundary = splitCellSpaceBoundary(
                                        adjacencyBoundary, doorLineString); // boundary 쪼갠다.
                                adjacencyBoundaryList.addAll(j, splitedBoundary);
                                j = j + splitedBoundary.size();

                                // 쪼개고 adjacencyBoundary를 참조하고 있던 다른 cell의 linestring을 찾아서
                                // 쪼개진 boundary로 바꿔줘야한다.
                                for (CellSpace otherCellSpace : cellSpaceMember) {
                                    if (cellSpace.equals(otherCellSpace))
                                        continue;
                                    ArrayList<LineString> otherLSElements = otherCellSpace
                                            .getLineStringElements();
                                    for (LineString otherLS : otherLSElements) {
                                        if (lineStringOfAdjacencyBoundaryMap.containsKey(otherLS)) {
                                            if (lineStringOfAdjacencyBoundaryMap.get(otherLS)
                                                    .contains(adjacencyBoundary)) {
                                                lineStringOfAdjacencyBoundaryMap.get(otherLS)
                                                        .remove(adjacencyBoundary);
                                                lineStringOfAdjacencyBoundaryMap.get(otherLS)
                                                        .addAll(splitedBoundary);
                                                otherCellSpace.getPartialBoundedBy().remove(
                                                        adjacencyBoundary);
                                                otherCellSpace.getPartialBoundedBy().
                                                		addAll(splitedBoundary);
                                            }
                                        }
                                    }
                                }

                                isSplitedByDoor = true;
                            } else if (GeometryUtil.isEqualsIgnoreReverseLineString(boundaryLS,
                                    doorLineString)
                                    && adjacencyBoundary.getBoundaryType() == BoundaryType.Door) {
                                isSplitedByDoor = true;
                            }
                        }

                        for (LineString split : splitedLS) {
                            ArrayList<Point> splitPoints = split.getPoints();
                            if (splitPoints.get(0).equalsPanelRatioXY(splitPoints.get(1))) {
                                continue;
                            }

                            for (CellSpaceBoundary boundary : adjacencyBoundaryList) {
                                if (GeometryUtil.isContainsLineString(split,
                                        boundary.getGeometry2D())
                                        || GeometryUtil.isOverlapsLineString(split,
                                                boundary.getGeometry2D())
                                        || GeometryUtil.isEqualsLineString(split,
                                                boundary.getGeometry2D())) {
                                    if (!lineStringOfAdjacencyBoundaryMap.containsKey(split)) {
                                        lineStringOfAdjacencyBoundaryMap.put(split,
                                                new ArrayList<CellSpaceBoundary>());
                                    }
                                    lineStringOfAdjacencyBoundaryMap.get(split).add(boundary);
                                    if (!boundaryOfReferenceCellSpaceMap.containsKey(boundary)) {
                                        boundaryOfReferenceCellSpaceMap.put(boundary,
                                                new ArrayList<CellSpace>());
                                    }
                                    if (!boundaryOfReferenceCellSpaceMap.get(boundary).contains(
                                            cellSpace)) {
                                        boundaryOfReferenceCellSpaceMap.get(boundary)
                                                .add(cellSpace);
                                    }
                                    if (!cellSpace.getPartialBoundedBy().contains(boundary)) {
                                        cellSpace.getPartialBoundedBy().add(boundary);
                                    }

                                    if (GeometryUtil.isEqualsLineString(split,
                                            boundary.getGeometry2D())) {
                                        boundary.getGeometry2D().setPoints(
                                                (ArrayList<Point>) split.getPoints().clone());
                                        boundary.getGeometry2D().setxLinkGeometry(split);
                                        boundary.getGeometry2D().setIsReversed(true);
                                    } else if (GeometryUtil.isEqualsIgnoreReverseLineString(split,
                                            boundary.getGeometry2D())) {
                                        boundary.getGeometry2D().setPoints(
                                                (ArrayList<Point>) split.getPoints().clone());
                                        boundary.getGeometry2D().setxLinkGeometry(split);
                                        boundary.getGeometry2D().setIsReversed(false);
                                    }
                                }
                            }
                        }

                        if (!isSplitedByDoor) { // 다른 벽과 인접해 있지만 인접한 곳이 아닌 벽의 부분에 문이 생긴 경우
                            CellSpaceBoundary newBoundary = new CellSpaceBoundary();
                            newBoundary.setBoundaryType(BoundaryType.Door);
                            newBoundary.setGeometry2D(doorLineString);

                            cellSpace.getPartialBoundedBy().add(newBoundary);

                            if (!lineStringOfAdjacencyBoundaryMap.containsKey(doorInThisCellSpace)) {
                                lineStringOfAdjacencyBoundaryMap.put(doorInThisCellSpace,
                                        new ArrayList<CellSpaceBoundary>());
                            }
                            lineStringOfAdjacencyBoundaryMap.get(doorInThisCellSpace).add(
                                    newBoundary);
                            if (!boundaryOfReferenceCellSpaceMap.containsKey(newBoundary)) {
                                boundaryOfReferenceCellSpaceMap.put(newBoundary,
                                        new ArrayList<CellSpace>());
                            }
                            boundaryOfReferenceCellSpaceMap.get(newBoundary).add(cellSpace);
                            project.getCurrentCellSpaceBoundaryOnFloor()
                                    .getCellSpaceBoundaryMember().add(newBoundary);
                        }

                    }

                    lineStringElements.remove(ls);
                    lineStringOfAdjacencyBoundaryMap.remove(ls);
                }
            }
            ArrayList<Point> exteriorPoints = cellSpace.getGeometry2D().getExteriorRing()
                    .getPoints();
            exteriorPoints.clear();
            for (LineString ls : cellSpace.getLineStringElements()) {
                exteriorPoints.add(ls.getPoints().get(0).clone());
            }
            exteriorPoints.add(exteriorPoints.get(0).clone());
        }
    }

    public ArrayList<CellSpaceBoundary> splitCellSpaceBoundary(CellSpaceBoundary boundary,
            LineString doorLineString) {
        HashMap<LineString, ArrayList<LineString>> xLink2DMap = project
                .getCurrentCellSpaceBoundaryOnFloor().getxLink2DMap();

        ArrayList<CellSpaceBoundary> newBoundaryList = new ArrayList<CellSpaceBoundary>();

        LineString boundaryLS = boundary.getGeometry2D();
        ArrayList<LineString> splited = GeometryUtil.splitLineString(boundaryLS, doorLineString);
        for(LineString split : splited) {
            for(Point p : split.getPoints()) {
                    setPanelRatioXY(p);
            }
        }
        for (LineString split : splited) {
            ArrayList<Point> splitPoints = split.getPoints();
            if (splitPoints.get(0).equalsPanelRatioXY(splitPoints.get(1)))
                continue;

            boolean result = true;
            for (Point splitPoint : splitPoints) {
            	boolean check = false;
            	for (Point doorPoint : doorLineString.getPoints()) {
            		if (doorPoint.equalsPanelRatioXY(splitPoint)) {
            			check = true;
            			break;
            		}
            	}
            	if (check == false) {
            		result = false;
            		break;
            	}
            }

            CellSpaceBoundary newBoundary = new CellSpaceBoundary();            
            if (result) {
                newBoundary.setBoundaryType(BoundaryType.Door);
            } else {
                newBoundary.setBoundaryType(BoundaryType.CellSpaceBoundary);
            }
            newBoundary.setGeometry2D(split);

            newBoundaryList.add(newBoundary);
        }

        /*
         * LineString geometry2D = boundary.getGeometry2D(); CellSpace cellSpace = boundaryOfCellSpaceMap.get(boundary); int indexOf =
         * cellSpace.getPartialBoundedBy().indexOf(boundary); ArrayList<Point> exteriorRingPoints =
         * cellSpace.getGeometry2D().getExteriorRing().getPoints(); int insertIndex = exteriorRingPoints.indexOf(geometry2D.getPoints().get(0)); int
         * count = 1; ArrayList<LineString> splited = splitLineString(geometry2D, doorLineString); // 기존 벽에 대한 기하를 문이 있는 기하로 3분할 for(LineString split
         * : splited) { ArrayList<Point> splitPoints = split.getPoints(); if(splitPoints.get(0).equals(splitPoints.get(1))) continue; // 분할했을 경우 문이 벽의
         * 끝에 있다면 끝점이 동일하므로 제외한다.
         * 
         * CellSpaceBoundary newBoundary = new CellSpaceBoundary(); // 새로운 CellSpaceBoundary 생성
         * if(doorLineString.getPoints().containsAll(splitPoints)) { newBoundary.setBoundaryType(BoundaryType.Door); } else {
         * newBoundary.setBoundaryType(BoundaryType.CellSpaceBoundary); } newBoundary.setGeometry2D(split);
         * 
         * cellSpace.getPartialBoundedBy().add(indexOf + count, newBoundary); // 기존에 있던 boundary 뒤에 insert, 기존 boundary는 나중에 삭제
         * exteriorRingPoints.add(insertIndex + count, split.getPoints().get(0).clone()); // cellSpace의 기하에 대해서도 문에 대한 좌표를 추가해야 한다. count++;
         * 
         * newBoundaryList.add(newBoundary); lineStringOfBoundaryMap.put(split, newBoundary); boundaryOfCellSpaceMap.put(newBoundary, cellSpace);
         * project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().add(newBoundary); } exteriorRingPoints.remove(insertIndex);
         * 
         * xLink2DMap.remove(geometry2D); lineStringOfBoundaryMap.remove(geometry2D); // 문 생성전 기존 boundary에 대한 기하와 boundary에 대한 정보는 삭제한다.
         * boundaryOfCellSpaceMap.remove(boundary); cellSpace.getPartialBoundedBy().remove(boundary);
         * project.getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember().remove(boundary);
         */

        return newBoundaryList;
    }

    /*
     * public ArrayList<LineString> splitLineString(LineString base, LineString target) { ArrayList<LineString> splited = new ArrayList<LineString>();
     * 
     * Point p1 = base.getPoints().get(0); Point p2 = base.getPoints().get(1); Point p3 = target.getPoints().get(0); Point p4 =
     * target.getPoints().get(1);
     * 
     * if(p1.getPanelRatioX() < p2.getPanelRatioX()) { if(p3.getPanelRatioX() > p4.getPanelRatioX()) { target.getPoints().clear();
     * target.getPoints().add(p4); target.getPoints().add(p3); } } else if(p1.getPanelRatioX() > p2.getPanelRatioX()) { if(p3.getPanelRatioX() <
     * p4.getPanelRatioX()) { target.getPoints().clear(); target.getPoints().add(p4); target.getPoints().add(p3); } } p3 = target.getPoints().get(0);
     * p4 = target.getPoints().get(1);
     * 
     * if(p1.getPanelRatioY() < p2.getPanelRatioY()) { if(p3.getPanelRatioY() > p4.getPanelRatioY()) { target.getPoints().clear();
     * target.getPoints().add(p4); target.getPoints().add(p3); } } else if(p1.getPanelRatioY() > p2.getPanelRatioY()) { if(p3.getPanelRatioY() <
     * p4.getPanelRatioY()) { target.getPoints().clear(); target.getPoints().add(p4); target.getPoints().add(p3); } } p3 = target.getPoints().get(0);
     * p4 = target.getPoints().get(1);
     * 
     * // create splited LineString of CellSpaceBoundary LineString newBase = new LineString(); newBase.setPoints((ArrayList<Point>)
     * base.getPoints().clone()); newBase.getPoints().remove(p2); newBase.getPoints().add(p3.clone());
     * 
     * LineString newLS = new LineString(); newLS.getPoints().add(p4.clone()); newLS.getPoints().add(p2);
     * 
     * splited.add(newBase); splited.add(target); splited.add(newLS);
     * 
     * return splited; }
     */
    public State searchAdjacencyState(MouseEvent e) {
        State adjacencyState = null;
        StateOnFloor stateOnFloor = project.getCurrentStateOnFloor();
        ArrayList<State> stateMember = stateOnFloor.getStateMember();
        for (State state : stateMember) {
            if (isAdjacencyPointToPoint(state.getPosition(), e.getX(), e.getY())) {
                adjacencyState = state;

                System.out.println("select state");
                break;
            }
        }

        return adjacencyState;
    }

    public CellSpace searchPointInCellSpace(MouseEvent e) {
        CellSpace pointInCellSpace = null;
        CellSpaceOnFloor cellSpaceOnFloor = project.getCurrentCellSpaceOnFloor();
        ArrayList<CellSpace> cellSpaceMember = cellSpaceOnFloor.getCellSpaceMember();
        for (CellSpace cellSpace : cellSpaceMember) {
        	Point p = new Point();
        	p.setPanelX(e.getX());
        	p.setPanelY(e.getY());
        	if (GeometryUtil.isContainsPolygon(cellSpace.getGeometry2D(), p)) {
            //if (isInPolygon(cellSpace.getGeometry2D(), e.getX(), e.getY())) {
                pointInCellSpace = cellSpace;

                System.out.println("select cellspace");
                break;
            }
        }

        return pointInCellSpace;
    }

    public Transition searchAdjacencyTransition(MouseEvent e) {
        Transition adjacencyTransition = null;
        TransitionOnFloor transitionOnFloor = project.getCurrentTransitionOnFloor();
        ArrayList<Transition> transitionMember = transitionOnFloor.getTransitionMember();
        for (Transition transition : transitionMember) {
            if (isAdjacencyPointToLineString(transition.getPath(), e.getX(), e.getY())) {
                adjacencyTransition = transition;

                System.out.println("select transition");
                break;
            }
        }

        return adjacencyTransition;
    }

    public CellSpaceBoundary searchAdjacencyCellSpaceBoundary(MouseEvent e) {
        CellSpaceBoundary adjacencyBoundary = null;
        CellSpaceBoundaryOnFloor cellSpaceBoundaryOnFloor = project
                .getCurrentCellSpaceBoundaryOnFloor();
        ArrayList<CellSpaceBoundary> cellSpaceBoundaryMember = cellSpaceBoundaryOnFloor
                .getCellSpaceBoundaryMember();
        for (CellSpaceBoundary boundary : cellSpaceBoundaryMember) {
            LineString lineString = null;
            if (boundary.getGeometry2D() == null)
                continue;
            if (boundary.getGeometry2D().getxLinkGeometry() == null) {
                lineString = boundary.getGeometry2D();
            } else {
                lineString = (LineString) boundary.getGeometry2D().getxLinkGeometry();
            }

            if (isAdjacencyPointToLineString(lineString, e.getX(), e.getY())) {
                adjacencyBoundary = boundary;

                System.out.println("select cellspaceBoundary");
                break;
            }
        }

        return adjacencyBoundary;
    }

    public Boolean searchSnapPointToCreatingCellSpace(MouseEvent e, ArrayList<Point> snapPointList) {
        Boolean foundSnapPoint = false;
        for (LineString lineString : cellSpaceCreatingLineStrings) {
            for (Point point : lineString.getPoints()) {
                if (isAdjacencyPointToPoint(point, e.getX(), e.getY())) {
                    snapPointList.add(point);
                    foundSnapPoint = true;
                }
            }
        }

        return foundSnapPoint;
    }
    
    public Point searchSnapPointToCellSpace(MouseEvent e) {
    	Map<String, Point> map = searchSnapPointToCellSpace(e, null);
    	Point p = map.get("Point");
    	
    	return p;
    }

    public Map searchSnapPointToCellSpace(MouseEvent e, LineString baseLine) {
    	Map map = new HashMap<String, Object>();
        Point snapPoint = null;
        CellSpaceOnFloor cellSpaceOnFloor = project.getCurrentCellSpaceOnFloor();
        ArrayList<CellSpace> cellSpaceMember = cellSpaceOnFloor.getCellSpaceMember();

        double minDistance = 10;
        LineString minLS = null;
        CellSpace minCS = null;
        for (CellSpace cellSpace : cellSpaceMember) {
        	if (cellSpace.getGmlID().equals("C2774")) {
        		System.out.println("C2774 found");
        	}
            double d = GeometryUtil.getDistancePointToPolygon(cellSpace.getGeometry2D(), e.getX(), e.getY());
            if(d > 100) continue;
            for (LineString ls : cellSpace.getLineStringElements()) {
                Point p1 = ls.getPoints().get(0);
                Point p2 = ls.getPoints().get(1);

                if (isAdjacencyPointToPoint(p1, e.getX(), e.getY())) {
                    snapPoint = new Point();
                    snapPoint.setPanelRatioX(p1.getPanelRatioX());
                    snapPoint.setPanelRatioY(p1.getPanelRatioY());
                    map.put("Point", snapPoint);
                    map.put("BaseLine", ls);
                    map.put("CellSpace", cellSpace);

                    System.out.println("snap point");
                } else if (isAdjacencyPointToPoint(p2, e.getX(), e.getY())) {
                    snapPoint = new Point();
                    snapPoint.setPanelRatioX(p2.getPanelRatioX());
                    snapPoint.setPanelRatioY(p2.getPanelRatioY());
                    map.put("Point", snapPoint);
                    map.put("BaseLine", ls);
                    map.put("CellSpace", cellSpace);

                    System.out.println("snap point");
                } 

                double distance = GeometryUtil.getDistancePointToLineString(ls, e.getX(), e.getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    minLS = ls;
                    minCS = cellSpace;
                }
            }
        }
        
        if (minCS != null && minLS != null && snapPoint == null) {
            Point p1 = minLS.getPoints().get(0);
            Point p2 = minLS.getPoints().get(1);
            setPanelXYForCurrentScale(p1);
            setPanelXYForCurrentScale(p2);
            double x1 = p1.getPanelX();
            double y1 = p1.getPanelY();
            double x2 = p2.getPanelX();
            double y2 = p2.getPanelY();
            // double x = ((double) e.getX() / floorPlanWidth * floorPlanScale);
            // double y = ((double) e.getY() / floorPlanHeight * floorPlanScale);

            // snapPoint = getSnapPointToLineString(x1, y1, x2, y2, e.getX(), e.getY());
            snapPoint = GeometryUtil.getSnapPointToLineString(x1, y1, x2, y2, e.getX(),
                    e.getY());
            if (snapPoint != null) {
                System.out.println("snapPointfound");
                setPanelRatioXY(snapPoint);
                baseLine = minLS;
                
                map.put("BaseLine", baseLine);
                map.put("Point", snapPoint);
                map.put("CellSpace", minCS);
                return map;
            }
        }

        return map;
    }

    public Point searchSnapPointToCellSpaceBoundary(MouseEvent e) {
        Point snapPoint = null;
        CellSpaceBoundaryOnFloor cellSpaceBoundaryOnFloor = project
                .getCurrentCellSpaceBoundaryOnFloor();
        ArrayList<CellSpaceBoundary> boundaryMember = cellSpaceBoundaryOnFloor
                .getCellSpaceBoundaryMember();
        for (CellSpaceBoundary boundary : boundaryMember) {
            LineString geometry2D = boundary.getGeometry2D();

            if (geometry2D == null)
                continue;
            if (geometry2D.getxLinkGeometry() != null)
                continue;

            Point p1 = null;
            try {
            	p1 = geometry2D.getPoints().get(0);
            } catch(Exception e1) {
            	e1.printStackTrace();
            }
            Point p2 = geometry2D.getPoints().get(1);

            if (isAdjacencyPointToPoint(p1, e.getX(), e.getY())) {
                snapPoint = new Point();
                snapPoint.setPanelRatioX(p1.getPanelRatioX());
                snapPoint.setPanelRatioY(p1.getPanelRatioY());

                System.out.println("snap point");
            } else if (isAdjacencyPointToPoint(p2, e.getX(), e.getY())) {
                snapPoint = new Point();
                snapPoint.setPanelRatioX(p2.getPanelRatioX());
                snapPoint.setPanelRatioY(p2.getPanelRatioY());

                System.out.println("snap point");
            }

            if (snapPoint == null) {
                setPanelXYForCurrentScale(p1);
                setPanelXYForCurrentScale(p2);
                double x1 = p1.getPanelX();
                double y1 = p1.getPanelY();
                double x2 = p2.getPanelX();
                double y2 = p2.getPanelY();
                // double x = ((double) e.getX() / floorPlanWidth * floorPlanScale);
                // double y = ((double) e.getY() / floorPlanHeight * floorPlanScale);

                // snapPoint = getSnapPointToLineString(x1, y1, x2, y2, e.getX(), e.getY());
                double distance = GeometryUtil.getDistancePointToLineString(geometry2D, e.getX(), e.getY());
                if (distance < 10) {
	                snapPoint = GeometryUtil.getSnapPointToLineString(x1, y1, x2, y2, e.getX(),
	                        e.getY());
	                if (snapPoint != null) {
	                    System.out.println("snapPointfound");
	                    return snapPoint;
	                }
                }
            }
        }

        return snapPoint;
    }

    public Point getSnapPointToLineString(int x1, int y1, int x2, int y2, int p, int q) {
        double snapBounds = 10;
        double distance = getDistancePointToLine(x1, y1, x2, y2, p, q);
        if (distance > snapBounds)
            return null;

        double a = y2 - y1;
        double b = x2 - x1;
        double t = (-1) * ((x1 - p) * b + (y1 - q) * a) / (Math.pow(a, 2) + Math.pow(b, 2));
        double x = t * b + x1;
        double y = t * a + y1;

        Point point = new Point();
        point.setPanelRatioX((double) x / (floorPlanWidth * floorPlanScale));
        point.setPanelRatioY((double) y / (floorPlanHeight * floorPlanScale));
        point.setPanelX(x);
        point.setPanelY(y);

        return point;
    }

    // /

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        this.requestFocus();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        isMouseDown = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

        isMouseDown = false;
        EditState currentEditState = project.getEditState();
        if (currentEditState == EditState.MOVE_STATE) {
            project.setEditState(EditState.SELECT_STATE);
        } else if (currentEditState == EditState.MOVE_CELLSPACE) {
            project.setEditState(EditState.SELECT_CELLSPACE);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int direction = e.getWheelRotation();
        double scale = project.getCurrentFloorPlanScale();

        if (currentKeyEvent != KeyEvent.VK_CONTROL){
            JScrollBar verticalScrollBar = mainFrame.getScrollPane().getVerticalScrollBar();
            int currentValue = verticalScrollBar.getValue();
            int increment = verticalScrollBar.getUnitIncrement();
            if(direction > 0) {
                verticalScrollBar.setValue(currentValue + increment * 2);
            } else {
                verticalScrollBar.setValue(currentValue - increment * 2);
            }
            return;
        }

        if (direction > 0) {
            if (scale > 1) {
                scale -= 0.5;
            }
        } else {
            if (scale < 80.0) {
                scale += 0.5;
            }
        }
        project.setCurrentFloorPlanScale(scale);

        mainFrame.resizePanelPrefferedDimension(
                (int) (project.getCurrentFloorPlan().getWidth() * scale), (int) (project
                        .getCurrentFloorPlan().getHeight() * scale));
        mainFrame.getScrollPane().revalidate();
        mainFrame.getScrollPane().repaint();
        
        //mainFrame.getScrollPane().getViewport().setViewPosition(new java.awt.Point(e.getX(), e.getY()));

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub
        if (project.getEditState() == null)
            return;
        EditState state = project.getEditState();
        EditWorkState workState = project.getEditWorkState();
        currentKeyEvent = e.getKeyCode();
        switch (e.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
            keyPressESCAPE(state);
            
            /*if (state == EditState.CREATE_STATE) {
                project.setEditState(EditState.NONE);
            } else if (state == EditState.CREATE_TRANSITION) {
                stateEnd1 = null;
                stateEnd2 = null;
                transitionPoints.clear();
            } else if (state == EditState.SELECT_STATE) {
                selectedState = null;
                selectedStateMap.clear();
            } else if (state == EditState.CREATE_INTERLAYERCONNECTION) {
                statesEnd1.clear();
                statesEnd2.clear();
            } else if (state == EditState.CREATE_CELLSPACE) {
                cellSpaceCreatingLineStrings.clear();
            } else if (state == EditState.SELECT_CELLSPACE) {
                selectedCellSpace = null;
                selectedCellSpaceMap.clear();
            } else if (state == EditState.CREATE_CELLSPACE_DUALITY) {

            } else if (state == EditState.CREATE_CELLSPACEBOUNDARY_DUALITY) {

            } else if (state == EditState.CREATE_TRANSITION_DUALITY) {

            } else if (state == EditState.CREATE_STATE_DUALITY) {

            } else if (state == EditState.CREATE_CELLSPACE_AS_DOOR) {

            } else if (state == EditState.CREATE_CELLSPACEBOUNDARY_AS_DOOR) {
                doorPointList.clear();
            }

            project.setEditState(EditState.NONE);
            mainFrame.setLabel_CurrentEditState("");*/
            break;
        case KeyEvent.VK_DELETE:
            if (state == EditState.SELECT_STATE) {
                // project.deleteState(selectedState);
                for (State selected : selectedStateMap.keySet()) {
                    project.deleteState(selected);
                }
                selectedState = null;
                selectedStateMap.clear();
                project.setEditState(EditState.NONE);
            } else if (state == EditState.SELECT_CELLSPACE) {
                for (CellSpace cellSpace : selectedCellSpaceMap.keySet()) {
                    project.deleteCellSpace(cellSpace);
                }
                selectedCellSpace = null;
                selectedCellSpaceMap.clear();
                project.setEditState(EditState.NONE);
            } else if (state == EditState.SELECT_TRANSITION) {
                for (Transition selected : selectedTransitionMap.keySet()) {
                    project.deleteTransition(selected);
                }
                selectedTransition = null;
                selectedTransitionMap.clear();
                project.setEditState(EditState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            } else if (state == EditState.SELECT_CELLSPACEBOUNDARY) {
            	for (CellSpaceBoundary selected : selectedCellSpaceBoundaryMap.keySet()) {
            		project.deleteCellSpaceBoundary(selected);;
            	}
            	selectedCellSpaceBoundary = null;
            	selectedCellSpaceBoundaryMap.clear();
            	project.setEditState(EditState.NONE);
            	mainFrame.setLabel_CurrentEditState("");
            }
            break;
        case KeyEvent.VK_ENTER:
            if (state == EditState.CREATE_INTERLAYERCONNECTION) {
                if (workState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND1) {
                    System.out.println("interlayerconnection_end2");
                    workState = EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND2;
                    project.setEditWorkState(workState);
                    mainFrame.setLabel_CurrentEditState("Create InterLayerConnection : Choose the other state(or states) and press Enter key");
                } else if (workState == EditWorkState.CREATE_INTERLAYERCONNECTION_SELECTEND2) {
                    workState = EditWorkState.CREATE_INTERLAYERCONNECTION_CREATE;
                    project.setEditWorkState(workState);
                    mainFrame.setLabel_CurrentEditState(workState.toString());
                    System.out.println("interlayerconnection_create");
                    //
                    createInterLayerConnection();

                    project.setEditState(EditState.NONE);
                    project.setEditWorkState(EditWorkState.NONE);
                    mainFrame.setLabel_CurrentEditState("");
                }
            } else if (state == EditState.CREATE_CELLSPACE
                    && cellSpaceCreatingLineStrings.size() >= 3) {
                CellSpace newCellSpace = createCellSpace(cellSpaceCreatingLineStrings);

                cellSpaceCreatingLineStrings.clear();

                project.setEditState(EditState.NONE);
                project.setEditWorkState(EditWorkState.NONE);
                mainFrame.setLabel_CurrentEditState("");
            }

            break;
        case KeyEvent.VK_CONTROL:
            System.out.println("pressed control");
            break;
        }

        repaint();
    }
    
    public void keyPressESCAPE(EditState state) {
        if (state == EditState.CREATE_STATE) {
            project.setEditState(EditState.NONE);
        } else if (state == EditState.CREATE_TRANSITION) {
            stateEnd1 = null;
            stateEnd2 = null;
            transitionPoints.clear();
        } else if (state == EditState.SELECT_STATE) {
            selectedState = null;
            selectedStateMap.clear();
        } else if (state == EditState.CREATE_INTERLAYERCONNECTION) {
            statesEnd1.clear();
            statesEnd2.clear();
        } else if (state == EditState.CREATE_CELLSPACE) {
            cellSpaceCreatingLineStrings.clear();
        } else if (state == EditState.SELECT_CELLSPACE) {
            selectedCellSpace = null;
            selectedCellSpaceMap.clear();
        } else if (state == EditState.CREATE_CELLSPACE_DUALITY) {

        } else if (state == EditState.CREATE_CELLSPACEBOUNDARY_DUALITY) {

        } else if (state == EditState.CREATE_TRANSITION_DUALITY) {

        } else if (state == EditState.CREATE_STATE_DUALITY) {

        } else if (state == EditState.CREATE_CELLSPACE_AS_DOOR) {

        } else if (state == EditState.CREATE_CELLSPACEBOUNDARY_AS_DOOR) {
            doorPointList.clear();
        }

        project.setEditState(EditState.NONE);
        mainFrame.setLabel_CurrentEditState("");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        currentKeyEvent = KeyEvent.KEY_RELEASED;
        System.out.println("release");
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        System.out.println("typed");
    }

    @Override
    protected void paintComponent(Graphics g) {
        // TODO Auto-generated method stub
        super.paintComponent(g);
        if (project == null)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2));

        if (project.getCurrentFloorPlan() != null) { // floorplan이 변경될 때만 출력하도록 변경
            // System.out.println(project.getCurrentFloorPlan());
            floorPlan = project.getCurrentFloorPlan();
            floorPlanWidth = floorPlan.getWidth();
            floorPlanHeight = floorPlan.getHeight();

            floorPlanScale = project.getCurrentFloorPlanScale();
            mainFrame.resizePanelPrefferedDimension((int) (floorPlanWidth * floorPlanScale),
                    (int) (floorPlanHeight * floorPlanScale));

            g.drawImage(floorPlan, 0, 0, (int) (floorPlanWidth * floorPlanScale),
                    (int) (floorPlanHeight * floorPlanScale), this);

            /*
             * System.out.println("width * scale : " + (floorPlanWidth * floorPlanScale)); System.out.println("height * scale : " + (floorPlanHeight *
             * floorPlanScale));
             */
            // g.drawImage(project.getCurrentFloorPlan(), 0, 0, floorPlanWidth, floorPlanHeight, this);
            // g.drawImage(project.getCurrentFloorPlan(), 0, 0, this.getWidth(),
            // this.getHeight(), this);
        }

        // display transition(creating transition)
        for (int i = 0; i < transitionPoints.size(); i++) {
            setPanelXYForCurrentScale(transitionPoints.get(i));
            double x1 = transitionPoints.get(i).getPanelX();
            double y1 = transitionPoints.get(i).getPanelY();
            g2.setColor(Color.YELLOW);
            g2.draw(new Ellipse2D.Double(x1 - 5, y1 - 5, 10, 10));
            // g.fillOval(x1 - 5, y1 - 5, 10, 10);

            if (i == transitionPoints.size() - 1)
                break;

            setPanelXYForCurrentScale(transitionPoints.get(i + 1));
            double x2 = transitionPoints.get(i + 1).getPanelX();
            double y2 = transitionPoints.get(i + 1).getPanelY();
            g2.draw(new Ellipse2D.Double(x2 - 5, y2 - 5, 10, 10));

            g2.setColor(Color.BLUE);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        // display transition
        if (project.getCurrentTransitionOnFloor() != null) {
            ArrayList<Transition> transitionMember = project.getCurrentTransitionOnFloor()
                    .getTransitionMember();
            for (Transition transition : transitionMember) {
                displayTransition(g, transition, Color.BLUE, floorPlanWidth, floorPlanHeight,
                        floorPlanScale);
            }
        }

        // display state
        if (project.getCurrentStateOnFloor() != null) {
            floorPlan = project.getCurrentFloorPlan();
            floorPlanScale = project.getCurrentFloorPlanScale();

            ArrayList<State> stateList = project.getCurrentStateOnFloor().getStateMember();
            for (State state : stateList) {
                displayState(g, state, Color.RED, floorPlanWidth, floorPlanHeight, floorPlanScale);
            }
        }
        
        // display points(creating cellspace)
        g2.setColor(Color.blue);
        for (LineString ls : cellSpaceCreatingLineStrings) {
            ArrayList<Point> lsPoints = ls.getPoints();
            for (int i = 0; i < lsPoints.size(); i++) {
                setPanelXYForCurrentScale(lsPoints.get(i));
                double x = lsPoints.get(i).getPanelX();
                double y = lsPoints.get(i).getPanelY();
                g2.draw(new Ellipse2D.Double(x - 5, y - 5, 10, 10));

                if (lsPoints.size() == 1)
                    break;

                setPanelXYForCurrentScale(lsPoints.get(i));
                setPanelXYForCurrentScale(lsPoints.get((i + 1) % lsPoints.size()));
                double x1 = lsPoints.get(i).getPanelX();
                double y1 = lsPoints.get(i).getPanelY();
                double x2 = lsPoints.get((i + 1) % lsPoints.size()).getPanelX();
                double y2 = lsPoints.get((i + 1) % lsPoints.size()).getPanelY();

                // geometry들에 drawble interface를 추가하여 geometry내부에서 화면에 표시처리하도록 변경
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
            }
        }

        // display cellspace
        if (project.getCurrentCellSpaceOnFloor() != null) {
            ArrayList<CellSpace> cellSpaceMember = project.getCurrentCellSpaceOnFloor()
                    .getCellSpaceMember();
            for (CellSpace cellSpace : cellSpaceMember) {
                displayCellSpace(g, cellSpace, Color.MAGENTA, floorPlanWidth, floorPlanHeight,
                        floorPlanScale);
            }
        }

        // display createing cellspaceboundary as door
        for (Point point : doorPointList) {
            setPanelXYForCurrentScale(point);
            double x = point.getPanelX();
            double y = point.getPanelY();
            g.setColor(Color.GREEN);
            g2.draw(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
        }

        if (project.getCurrentCellSpaceBoundaryOnFloor() != null) {
            ArrayList<CellSpaceBoundary> cellSpaceBoundaryMember = project
                    .getCurrentCellSpaceBoundaryOnFloor().getCellSpaceBoundaryMember();
            for (CellSpaceBoundary boundary : cellSpaceBoundaryMember) {
                if (boundary.getBoundaryType() == BoundaryType.Door) {
                    displayCellSpaceBoundary(g, boundary, Color.GREEN, floorPlanWidth,
                            floorPlanHeight, floorPlanScale);
                }
            }
        }

        if (!selectedStateMap.isEmpty()) {
            for (State state : selectedStateMap.keySet()) {
                displayState(g, state, selectedStateMap.get(state), floorPlanWidth,
                        floorPlanHeight, floorPlanScale);
            }
        }
        if (!selectedTransitionMap.isEmpty()) {
            for (Transition transition : selectedTransitionMap.keySet()) {
                displayTransition(g, transition, selectedTransitionMap.get(transition),
                        floorPlanWidth, floorPlanHeight, floorPlanScale);
            }
        }
        if (!selectedCellSpaceMap.isEmpty()) {
            for (CellSpace cellSpace : selectedCellSpaceMap.keySet()) {
                displayCellSpace(g, cellSpace, selectedCellSpaceMap.get(cellSpace), floorPlanWidth,
                        floorPlanHeight, floorPlanScale);
                for(CellSpaceBoundary boundary : cellSpace.getPartialBoundedBy()) {
                    selectedCellSpaceBoundaryMap.put(boundary, Color.CYAN);
                }
            }
        }
        if (!selectedCellSpaceBoundaryMap.isEmpty()) {
            for (CellSpaceBoundary boundary : selectedCellSpaceBoundaryMap.keySet()) {
                displayCellSpaceBoundary(g, boundary, selectedCellSpaceBoundaryMap.get(boundary),
                        floorPlanWidth, floorPlanHeight, floorPlanScale);
            }
        }
    }

    public void displayState(Graphics g, State state, Color color, int floorPlanWidth,
            int floorPlanHeight, double floorPlanScale) {
        int x = (int) (state.getPosition().getPanelRatioX() * floorPlanWidth * floorPlanScale);
        int y = (int) (state.getPosition().getPanelRatioY() * floorPlanHeight * floorPlanScale);
        if (hasInterLayerConnection(state)) {
            g.setColor(Color.PINK);
        } else {
            g.setColor(color);
        }
        g.fillOval(x - 5, y - 5, 10, 10);
    }

    public void displayTransition(Graphics g, Transition transition, Color color,
            int floorPlanWidth, int floorPlanHeight, double floorPlanScale) {
        ArrayList<State> stateMember = project.getCurrentStateOnFloor().getStateMember();
        State[] states = transition.getStates();

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(color);

        ArrayList<Point> points = transition.getPath().getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            setPanelXYForCurrentScale(points.get(i));
            setPanelXYForCurrentScale(points.get(i + 1));
            double x1 = points.get(i).getPanelX();
            double y1 = points.get(i).getPanelY();
            double x2 = points.get(i + 1).getPanelX();
            double y2 = points.get(i + 1).getPanelY();

            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        // geometry들에 drawble interface를 추가하여 geometry내부에서 화면에 표시처리하도록 변경
        double x = 0, y = 0;
        if (!stateMember.contains(states[0])) { // 다른 층의 state와 연결되었을 경우
            setPanelXYForCurrentScale(states[0].getPosition());

            x = states[0].getPosition().getPanelX();
            y = states[0].getPosition().getPanelY();
            g2.setColor(Color.lightGray);
            g2.draw(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
        } else if (!stateMember.contains(states[1])) {
            setPanelXYForCurrentScale(states[1].getPosition());

            x = states[1].getPosition().getPanelX();
            y = states[1].getPosition().getPanelY();
            g2.setColor(Color.lightGray);
            g2.draw(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
        }
    }

    public void displayCellSpace(Graphics g, CellSpace cellSpace, Color color, int floorPlanWidth,
            int floorPlanHeight, double floorPlanScale) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(color);

        ArrayList<Point> points = cellSpace.getGeometry2D().getExteriorRing().getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
        	if (points.get(i).getPanelRatioX() == 0 && points.get(i).getPanelRatioY() == 0) {
        		setPanelRatioXY(points.get(i));
        	}
        	if (points.get(i + 1).getPanelRatioX() == 0 && points.get(i + 1).getPanelRatioY() == 0) {
        		setPanelRatioXY(points.get(i + 1));
        	}
        	
            setPanelXYForCurrentScale(points.get(i));
            setPanelXYForCurrentScale(points.get(i + 1));

            double x1 = points.get(i).getPanelX();
            double y1 = points.get(i).getPanelY();
            double x2 = points.get(i + 1).getPanelX();
            double y2 = points.get(i + 1).getPanelY();

            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        
        ArrayList<LineString> lineStringElements = cellSpace.getLineStringElements();
        for (int i = 0; i < lineStringElements.size(); i++) {
        	LineString lineString = lineStringElements.get(i);
        	ArrayList<Point> lsPoints = lineString.getPoints();
        	for (Point point : lsPoints) {
        		setPanelXYForCurrentScale(point);
        	}
        }
    }

    public void displayCellSpaceBoundary(Graphics g, CellSpaceBoundary boundary, Color color,
            int floorPlanWidth, int floorPlanHeight, double floorPlanScale) {
        if (boundary.getGeometry2D() == null)
            return; // 3D 기하를 위한 윗면, 아랫면 boundary는 2D 기하가 없다.

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(5));
        g2.setColor(color);

        LineString lineString = null;
        if (boundary.getGeometry2D().getxLinkGeometry() != null) {
            lineString = (LineString) boundary.getGeometry2D().getxLinkGeometry();
        } else {
            lineString = boundary.getGeometry2D();
        }

        ArrayList<Point> points = lineString.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            setPanelXYForCurrentScale(points.get(i));
            setPanelXYForCurrentScale(points.get(i + 1));
            double x1 = points.get(i).getPanelX();
            double y1 = points.get(i).getPanelY();
            double x2 = points.get(i + 1).getPanelX();
            double y2 = points.get(i + 1).getPanelY();

            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }

    public boolean hasInterLayerConnection(State state) {
        ArrayList<InterLayerConnection> ilcMember = project.getMultiLayeredGraph().getInterEdges()
                .get(0).getInterLayerConnectionMember();
        for (InterLayerConnection ilc : ilcMember) {
            State[] interConnects = ilc.getInterConnects();
            if (interConnects[0] == state || interConnects[1] == state) {
                return true;
            }
        }

        return false;
    }

    // /////////////// point in polygon for select cellspace ///////////////////
    public double area2(Point p1, Point p2, Point p3) {
        return ((p2.getPanelRatioX() - p1.getPanelRatioX()) * (p3.getPanelRatioY() - p1
                .getPanelRatioY()))
                - ((p3.getPanelRatioX() - p1.getPanelRatioX()) * (p2.getPanelRatioY() - p1
                        .getPanelRatioY()));
    }

    public boolean left(Point p1, Point p2, Point p3) {
        return area2(p1, p2, p3) > 0;
    }

    public boolean leftOn(Point p1, Point p2, Point p3) {
        return area2(p1, p2, p3) >= 0;
    }

    public boolean colLinear(Point p1, Point p2, Point p3) {
        double upsilon = 0.00001;
        // return area2(p1, p2, p3) == 0;
        return Math.abs(area2(p1, p2, p3)) <= upsilon;
    }

    public boolean colLinear(LineString line1, LineString line2) {
        Point p1 = line1.getPoints().get(0);
        Point p2 = line1.getPoints().get(1);
        Point p3 = line2.getPoints().get(0);
        Point p4 = line2.getPoints().get(1);

        double minx1 = 0, miny1 = 0, minx2 = 0, miny2 = 0;
        double maxx1 = 0, maxy1 = 0, maxx2 = 0, maxy2 = 0;

        if (p1.getPanelRatioX() < p2.getPanelRatioX()) {
            minx1 = p1.getPanelRatioX();
            maxx1 = p2.getPanelRatioX();
        } else {
            minx1 = p2.getPanelRatioX();
            maxx1 = p1.getPanelRatioX();
        }
        if (p1.getPanelRatioY() < p2.getPanelRatioY()) {
            miny1 = p1.getPanelRatioY();
            maxy1 = p2.getPanelRatioY();
        } else {
            miny1 = p2.getPanelRatioY();
            maxy1 = p1.getPanelRatioY();
        }
        if (p3.getPanelRatioX() < p4.getPanelRatioX()) {
            minx2 = p3.getPanelRatioX();
            maxx2 = p4.getPanelRatioX();
        } else {
            minx2 = p4.getPanelRatioX();
            maxx2 = p3.getPanelRatioX();
        }
        if (p3.getPanelRatioY() < p4.getPanelRatioY()) {
            miny2 = p3.getPanelRatioY();
            maxy2 = p4.getPanelRatioY();
        } else {
            miny2 = p4.getPanelRatioY();
            maxy2 = p3.getPanelRatioY();
        }

        if (minx1 == maxx2) {
            if (maxy1 <= miny2) {
                return false;
            }
        }
        if (miny1 == maxy2) {
            if (maxx1 <= minx2) {
                return false;
            }
        }
        /*
         * if((minx1 != maxx1 && minx2 != maxx2) && (maxx2 <= minx1 || maxx1 <= minx2)) { return false; } else if((miny1 != maxy1 && miny2 != maxy2)
         * && (maxy2 <= miny1 || maxy1 <= miny2)) { return false; }
         */
        if ((colLinear(p1, p2, p4) && colLinear(p3, p4, p1))
                || (colLinear(p1, p2, p3) && colLinear(p3, p4, p2))
                || (colLinear(p1, p2, p4) && colLinear(p1, p2, p3))
                || (colLinear(p3, p4, p1) && colLinear(p3, p4, p2))) {
            return true;
        }

        return false;
    }

    public boolean intersectProp(Point p1, Point p2, Point p3, Point p4) {
        if (colLinear(p1, p2, p3) || colLinear(p1, p2, p4) || colLinear(p3, p4, p1)
                || colLinear(p3, p4, p2)) {
            return false;
        }

        return xor(left(p1, p2, p3), left(p1, p2, p4)) && xor(left(p3, p4, p1), left(p3, p4, p2));
    }

    public boolean xor(boolean x, boolean y) {
        return !x ^ !y;
    }

    boolean between(Point p1, Point p2, Point p3) {
        if (!colLinear(p1, p2, p3))
            return false;

        if (p1.getPanelRatioX() != p2.getPanelRatioX()) {
            return ((p1.getPanelRatioX() <= p3.getPanelRatioX()) && (p3.getPanelRatioX() <= p2
                    .getPanelRatioX()))
                    || ((p1.getPanelRatioX() >= p3.getPanelRatioX()) && (p3.getPanelRatioX() >= p2
                            .getPanelRatioX()));
        }

        return ((p1.getPanelRatioY() <= p3.getPanelRatioY()) && (p3.getPanelRatioY() <= p2
                .getPanelRatioY()))
                || ((p1.getPanelRatioY() >= p3.getPanelRatioY()) && (p3.getPanelRatioY() >= p2
                        .getPanelRatioY()));
    }

    public boolean intersect(Point[] line1, Point[] line2) {
        if (intersectProp(line1[0], line1[1], line2[0], line2[1]))
            return true;
        else if (between(line1[0], line1[1], line2[0]) || between(line1[0], line1[1], line2[1])
                || between(line2[0], line2[1], line1[0]) || between(line2[0], line2[1], line1[1]))
            return true;

        return false;
    }

    public boolean isInPolygon(Polygon polygon, int x, int y) {
        boolean isInside = false;
        int count = 0;

        ArrayList<Point> points = polygon.getExteriorRing().getPoints();

        // swing의 좌표계로 볼 때 반시계 방향인 polygon은
        // 왼쪽 아래가 0, 0인 좌표계로 뒤집으면 시계 방향이 된다.
        // 그런데 아래 intersect 함수는 반시계 방향인 기하에 대한 것이므로
        // 좌표 순서가 반시계 방향이 되도록 역순으로 검사를 한다.
        // 실제 gml로 출력할 때도 좌표계를 뒤집어서 올바르게 출력이 되도록 한다.
        for (int i = points.size() - 1; i > 0; i--) {
            Point p1 = points.get(i);
            Point p2 = points.get(i - 1);
            Point p3 = new Point();
            Point p4 = new Point();
            p3.setPanelRatioX((double) x / (floorPlanWidth * floorPlanScale));
            p3.setPanelRatioY((double) y / (floorPlanHeight * floorPlanScale));
            p4.setPanelRatioX(1);
            p4.setPanelRatioY(1);

            Point[] line1 = new Point[] { p1, p2 };
            Point[] line2 = new Point[] { p3, p4 };
            if (intersect(line1, line2)) {
                count++;
            }
        }
        if (count % 2 == 1)
            isInside = true;
        else
            isInside = false;

        return isInside;
    }

    // /////////////
    public boolean isAdjacencyPointToPoint(Point point, int x, int y) {
        boolean isAdjacency = false;
        double snapBounds = 10;

        if (point.getPanelRatioX() == 0 && point.getPanelRatioY() == 0) {
        	setPanelRatioXY(point);
        }
        setPanelXYForCurrentScale(point);
        double pointX = point.getPanelX();
        double pointY = point.getPanelY();
        /*
         * System.out.println("stateX : " + pointX); System.out.println("stateY : " + pointY); System.out.println("x : " + x);
         * System.out.println("y : " + y);
         */
        if ((Math.abs(pointX - x) <= snapBounds) && (Math.abs(pointY - y) <= snapBounds)) {
            isAdjacency = true;
        }

        return isAdjacency;
    }

    public boolean isAdjacencyPointToLineString(LineString lineString, double x, double y) {
        boolean isAdjacency = false;
        double snapBounds = 10;

        ArrayList<Point> points = lineString.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            if (p1.getPanelRatioX() == 0 && p1.getPanelRatioY() == 0) {
            	setPanelRatioXY(p1);
            }
            if (p2.getPanelRatioX() == 0 && p2.getPanelRatioY() == 0) {
            	setPanelRatioXY(p2);
            }
            
            setPanelXYForCurrentScale(p1);
            setPanelXYForCurrentScale(p2);
            double x1 = p1.getPanelX();
            double y1 = p1.getPanelY();
            double x2 = p2.getPanelX();
            double y2 = p2.getPanelY();

            double lowerX, lowerY, upperX, upperY;
            if (x1 < x2) {
                lowerX = x1;
                upperX = x2;
            } else {
                lowerX = x2;
                upperX = x1;
            }
            if (y1 < y2) {
                lowerY = y1;
                upperY = y2;
            } else {
                lowerY = y2;
                upperY = y1;
            }
            
            double epsilon = 5;
            boolean coverX = false;
            if (upperX - lowerX <= epsilon) {
            	coverX = true;
            } else {
            	if (lowerX <= x && x <= upperX) {
            		coverX = true;
            	}
            }
            boolean coverY = false;
            if (upperY - lowerY <= epsilon) {
            	coverY = true;
            } else {
            	if (lowerY <= y && y <= upperY) {
            		coverY = true;
            	}
            }
            

            if (!(coverX && coverY)) {
            	continue;
            }
            //if (!(lowerX <= x && x <= upperX && lowerY <= y && y <= upperY))
            //    continue;

            // double distance = getDistancePointToLine(x1, y1, x2, y2, x, y);
            double distance = GeometryUtil.getDistancePointToLine(x1, y1, x2, y2, x, y);
            if (distance <= snapBounds) {
                isAdjacency = true;
            }
        }

        return isAdjacency;
    }

    public double getDistancePointToLine(double x1, double y1, double x2, double y2, double p,
            double q) {
        double a = y1 - y2;
        double b = x2 - x1;
        double c = a * (-1) * x1 - b * y1;
        double distance = Math.abs(a * p + b * q + c) / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));

        return distance;
    }

    public void setPanelXYForCurrentScale(Point p) {
        double x = (p.getPanelRatioX() * floorPlanWidth * floorPlanScale);
        double y = (p.getPanelRatioY() * floorPlanHeight * floorPlanScale);

        p.setPanelX(x);
        p.setPanelY(y);
    }

    public void setPanelRatioXY(Point p) {
        double x = p.getPanelX();
        double y = p.getPanelY();

        double ratioX = ((double) x / (floorPlanWidth * floorPlanScale));
        double ratioY = ((double) y / (floorPlanHeight * floorPlanScale));
        /*
        DecimalFormat format = new DecimalFormat(".#####");
        String realXStr = format.format(ratioX);
        String realYStr = format.format(ratioY);
        ratioX = Double.parseDouble(realXStr);
        ratioY = Double.parseDouble(realYStr);
        */
        p.setPanelRatioX(ratioX);
        p.setPanelRatioY(ratioY);
    }

    public void createInterLayerConnection() {
        System.out.println("call createInteRlayerConnection");
        InterLayerConnectionDialog dialog = new InterLayerConnectionDialog(mainFrame, project,
                statesEnd1, statesEnd2, spaceLayerEnd1, spaceLayerEnd2);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        selectedStateMap.clear();
        statesEnd1.clear();
        statesEnd2.clear();
        spaceLayerEnd1 = null;
        spaceLayerEnd2 = null;

        repaint();
    }

    public ProjectFile getProject() {
        return project;
    }

    public void setProject(ProjectFile project) {
        this.project = project;
    }

    public void displayStateID() {

    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public State getStateEnd1() {
        return stateEnd1;
    }

    public void setStateEnd1(State stateEnd1) {
        this.stateEnd1 = stateEnd1;
    }

    public State getStateEnd2() {
        return stateEnd2;
    }

    public void setStateEnd2(State stateEnd2) {
        this.stateEnd2 = stateEnd2;
    }

    public SpaceLayer getSpaceLayerEnd1() {
        return spaceLayerEnd1;
    }

    public void setSpaceLayerEnd1(SpaceLayer spaceLayerEnd1) {
        this.spaceLayerEnd1 = spaceLayerEnd1;
    }

    public SpaceLayer getSpaceLayerEnd2() {
        return spaceLayerEnd2;
    }

    public void setSpaceLayerEnd2(SpaceLayer spaceLayerEnd2) {
        this.spaceLayerEnd2 = spaceLayerEnd2;
    }

    public ArrayList<State> getStatesEnd1() {
        return statesEnd1;
    }

    public void setStatesEnd1(ArrayList<State> statesEnd1) {
        this.statesEnd1 = statesEnd1;
    }

    public ArrayList<State> getStatesEnd2() {
        return statesEnd2;
    }

    public void setStatesEnd2(ArrayList<State> statesEnd2) {
        this.statesEnd2 = statesEnd2;
    }

    public State getSelectedState() {
        return selectedState;
    }

    public void setSelectedState(State selectedState) {
        this.selectedState = selectedState;
    }

    public HashMap<State, Color> getSelectedStateMap() {
        return selectedStateMap;
    }

    public void setSelectedStateMap(HashMap<State, Color> selectedStateMap) {
        this.selectedStateMap = selectedStateMap;
    }

    public BufferedImage getFloorPlan() {
        return floorPlan;
    }

    public void setFloorPlan(BufferedImage floorPlan) {
        this.floorPlan = floorPlan;
    }

    public int getFloorPlanWidth() {
        return floorPlanWidth;
    }

    public void setFloorPlanWidth(int floorPlanWidth) {
        this.floorPlanWidth = floorPlanWidth;
    }

    public int getFloorPlanHeight() {
        return floorPlanHeight;
    }

    public void setFloorPlanHeight(int floorPlanHeight) {
        this.floorPlanHeight = floorPlanHeight;
    }

    public double getFloorPlanScale() {
        return floorPlanScale;
    }

    public void setFloorPlanScale(double floorPlanScale) {
        this.floorPlanScale = floorPlanScale;
    }

    public boolean isMouseDown() {
        return isMouseDown;
    }

    public void setMouseDown(boolean isMouseDown) {
        this.isMouseDown = isMouseDown;
    }

    public int getCurrentKeyEvent() {
        return currentKeyEvent;
    }

    public void setCurrentKeyEvent(int currentKeyEvent) {
        this.currentKeyEvent = currentKeyEvent;
    }

    private JPopupMenu getPopupMenu_State() {
        if (popupMenu_State == null) {
            popupMenu_State = new JPopupMenu();
            popupMenu_State.add(getMntmStateDuality());
            popupMenu_State.add(getMntmStateProperties());
        }
        return popupMenu_State;
    }

    private static void addPopup(Component component, final JPopupMenu popup) {
        component.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void showPropertiesDialog(String type, AbstractFeature feature) {
            JDialog dialog = null;
            if(type.equalsIgnoreCase("STATE")) {
            	dialog = new StatePropertiesDialog((State) feature, project.getMultiLayeredGraph().getInterEdges().get(0));
            } else if(type.equalsIgnoreCase("TRANSITION")) {
            	dialog = new TransitionPropertiesDialog((Transition) feature);
            } else if(type.equalsIgnoreCase("CELLSPACE")) {
            	dialog = new CellSpacePropertiesDialog((CellSpace) feature);
            } else if(type.equalsIgnoreCase("CELLSPACEBOUNDARY")) {
            	dialog = new CellSpaceBoundaryPropertiesDialog((CellSpaceBoundary) feature);
            }
            
            dialog.setModal(true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            
            if (feature instanceof CellSpace) {
            	CellSpace cellSpace = (CellSpace) feature;
            	if (cellSpace.getDescription("Usage").equals("Door")) {
            		double groundHeight = project.getCurrentCellSpaceOnFloor().getFloorProperty().getGroundHeight();
            		double doorHeight = project.getCurrentCellSpaceOnFloor().getFloorProperty().getDoorHeight();
            		cellSpace.setCeilingHeight(groundHeight + doorHeight);
            		cellSpace.setIsDefaultCeiling(false);
            		
            		for (CellSpaceBoundary bounded : cellSpace.getPartialBoundedBy()) {
            			bounded.setBoundaryType(BoundaryType.Door);
            		}
            	}
            } else if (feature instanceof CellSpaceBoundary) {
            	CellSpaceBoundary boundary = (CellSpaceBoundary) feature;
            	if (boundary.getBoundaryType() == BoundaryType.Door) {
            		//
            	}
            }
            
            System.out.println("properties end");
            repaint();
    }
    private JMenuItem getMntmStateDuality() {
        if (mntmStateDuality == null) {
            mntmStateDuality = new JMenuItem("Duality");
            mntmStateDuality.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    project.setEditState(EditState.CREATE_STATE_DUALITY);
                }
            });
        }
        return mntmStateDuality;
    }

    private JPopupMenu getPopupMenu_CellSpace() {
        if (popupMenu_CellSpace == null) {
            popupMenu_CellSpace = new JPopupMenu();
            popupMenu_CellSpace.add(getMntmCellSpaceDuality());
            popupMenu_CellSpace.add(getMntmCellSpaceProperties());
        }
        return popupMenu_CellSpace;
    }

    private JMenuItem getMntmCellSpaceDuality() {
        if (mntmCellSpaceDuality == null) {
            mntmCellSpaceDuality = new JMenuItem("Duality");
            mntmCellSpaceDuality.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    project.setEditState(EditState.CREATE_CELLSPACE_DUALITY);
                }
            });
        }
        return mntmCellSpaceDuality;
    }

    private JPopupMenu getPopupMenu_Transition() {
        if (popupMenu_Transition == null) {
            popupMenu_Transition = new JPopupMenu();
            popupMenu_Transition.add(getMntmTransitionDuality());
            popupMenu_Transition.add(getMntmTransitionProperties());
        }
        return popupMenu_Transition;
    }

    private JMenuItem getMntmTransitionDuality() {
        if (mntmTransitionDuality == null) {
            mntmTransitionDuality = new JMenuItem("Duality");
            mntmTransitionDuality.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    project.setEditState(EditState.CREATE_TRANSITION_DUALITY);
                }
            });
        }
        return mntmTransitionDuality;
    }

    private JPopupMenu getPopupMenu_CellSpaceBoundary() {
        if (popupMenu_CellSpaceBoundary == null) {
            popupMenu_CellSpaceBoundary = new JPopupMenu();
            popupMenu_CellSpaceBoundary.add(getMntmCellSpaceBoundaryDuality());
            popupMenu_CellSpaceBoundary.add(getMntmCellSpaceBoundaryProperties());
        }
        return popupMenu_CellSpaceBoundary;
    }

    private JMenuItem getMntmCellSpaceBoundaryDuality() {
        if (mntmCellSpaceBoundaryDuality == null) {
            mntmCellSpaceBoundaryDuality = new JMenuItem("Duality");
            mntmCellSpaceBoundaryDuality.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    project.setEditState(EditState.CREATE_CELLSPACEBOUNDARY_DUALITY);
                }
            });
        }
        return mntmCellSpaceBoundaryDuality;
    }

    private JMenuItem getMntmStateProperties() {
        if (mntmStateProperties == null) {
            mntmStateProperties = new JMenuItem("Properties");
            mntmStateProperties.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    showPropertiesDialog("State", selectedState);
                }
            });
        }
        return mntmStateProperties;
    }

    private JMenuItem getMntmCellSpaceProperties() {
        if (mntmCellSpaceProperties == null) {
            mntmCellSpaceProperties = new JMenuItem("Properties");
            mntmCellSpaceProperties.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPropertiesDialog("CellSpace", selectedCellSpace);
                }
            });
        }
        return mntmCellSpaceProperties;
    }

    private JMenuItem getMntmTransitionProperties() {
        if (mntmTransitionProperties == null) {
            mntmTransitionProperties = new JMenuItem("Properties");
            mntmTransitionProperties.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPropertiesDialog("Transition", selectedTransition);
                }
            });
        }
        return mntmTransitionProperties;
    }

    private JMenuItem getMntmCellSpaceBoundaryProperties() {
        if (mntmCellSpaceBoundaryProperties == null) {
            mntmCellSpaceBoundaryProperties = new JMenuItem("Properties");
            mntmCellSpaceBoundaryProperties.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPropertiesDialog("CellSpaceBoundary", selectedCellSpaceBoundary);
                }
            });
        }
        return mntmCellSpaceBoundaryProperties;
    }
    
    public void searchByID(String ID) {
        ArrayList<CellSpace> cellSpaces = project.getCurrentCellSpaceOnFloor().getCellSpaceMember();
        //selectedCellSpaceMap.clear();
        for(CellSpace cellSpace : cellSpaces) {
                if(cellSpace.getGmlID().equalsIgnoreCase(ID)) {
                        project.setEditState(EditState.SELECT_CELLSPACE);
                		selectedCellSpaceMap.put(cellSpace, Color.yellow);
                        System.out.println("searched");
                }
        }
        repaint();
    }
    
    // for joonseokkim
    public void setPanelXYForCurrentScaleForJSK() {
    	IndoorFeatures indoorFeatures = project.getIndoorFeatures();
    	PrimalSpaceFeatures primalSpaceFeatures = indoorFeatures.getPrimalSpaceFeatures();
    	ArrayList<CellSpaceOnFloor> cellSpaceOnFloorList = primalSpaceFeatures.getCellSpaceOnFloors();
    	System.out.println("CellSpaceOnFloorList size : " + cellSpaceOnFloorList.size());
    	for (CellSpaceOnFloor cellSpaceOnFloor : cellSpaceOnFloorList) {
    		FloorProperty floorProperty = cellSpaceOnFloor.getFloorProperty();
    		
    		File floorPlanFile = new File(floorProperty.getFloorPlanPath());
            BufferedImage floorPlan = null;
            double width;
            double height;
            if (floorPlanFile.exists()) {
                try {
					floorPlan = ImageIO.read(floorPlanFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            if (floorPlan == null) {
            	System.out.println("floorPlan is null");
            }
            
			width = floorPlan.getWidth();
			height = floorPlan.getHeight();
			
			ArrayList<CellSpace> cellSpaceMember = cellSpaceOnFloor.getCellSpaceMember();
			for (CellSpace cellSpace : cellSpaceMember) {
				Polygon geometry2D = cellSpace.getGeometry2D();
				LinearRing exteriorRing = geometry2D.getExteriorRing();
				setPanelXYForCurrentScaleForJSK(exteriorRing, width, height, floorPlanScale);
				
				ArrayList<LineString> lineStringElements = cellSpace.getLineStringElements(); // (p1,p2) (p2,p3) (p3,p1)
				for (int i = 0; i < exteriorRing.getPoints().size() - 1; i++) { // p1 p2 p3 p1
					int previous = i - 1;
					if (previous == -1) {
						previous = exteriorRing.getPoints().size() - 2;
					}
					Point p = exteriorRing.getPoints().get(i);
					Point prevP = lineStringElements.get(previous).getPoints().get(1);
					Point nextP = lineStringElements.get(i).getPoints().get(0);
					
					prevP.copy(p);
					nextP.copy(p);					
				}
				/*
				for (LineString ls : lineStringElements) {
					setPanelXYForCurrentScaleForJSK(ls, width, height, floorPlanScale);
				}
				*/
			}
    	}
    	
    	MultiLayeredGraph mlg = indoorFeatures.getMultiLayeredGraph();
    	SpaceLayers spaceLayers = mlg.getSpaceLayers().get(0);
    	SpaceLayer spaceLayer = spaceLayers.getSpaceLayerMember().get(0);
    	ArrayList<StateOnFloor> stateOnFloorList = spaceLayer.getNodes().get(0).getStateOnFloors();
    	for (StateOnFloor stateOnFloor : stateOnFloorList) {
    		FloorProperty floorProperty = stateOnFloor.getFloorProperty();
    		
    		File floorPlanFile = new File(floorProperty.getFloorPlanPath());
            BufferedImage floorPlan = null;
            double width;
            double height;
            if (floorPlanFile.exists()) {
                try {
					floorPlan = ImageIO.read(floorPlanFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            if (floorPlan == null) {
            	System.out.println("floorPlan is null");
            }
            
			width = floorPlan.getWidth();
			height = floorPlan.getHeight();
			
			ArrayList<State> stateMember = stateOnFloor.getStateMember();
			for (State state : stateMember) {
				Point p = state.getPosition();
				setPanelXYForCurrentScaleForJSK(p, width, height, floorPlanScale);
			}
    	}
    	
    	ArrayList<TransitionOnFloor> transitionOnFloorList = spaceLayer.getEdges().get(0).getTransitionOnFloors();
    	System.out.println("transitionOnFloorList size : " + transitionOnFloorList.size());
    	for (TransitionOnFloor transitionOnFloor : transitionOnFloorList) {
    		FloorProperty floorProperty = transitionOnFloor.getFloorProperty();
    		
    		File floorPlanFile = new File(floorProperty.getFloorPlanPath());
            BufferedImage floorPlan = null;
            double width;
            double height;
            if (floorPlanFile.exists()) {
                try {
					floorPlan = ImageIO.read(floorPlanFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            if (floorPlan == null) {
            	System.out.println("floorPlan is null");
            }
            
			width = floorPlan.getWidth();
			height = floorPlan.getHeight();
			
			ArrayList<Transition> transitionMember = transitionOnFloor.getTransitionMember();
			for (Transition transition : transitionMember) {
				LineString ls = transition.getPath();
				setPanelXYForCurrentScaleForJSK(ls, width, height, floorPlanScale);
			}
    	}
    }
    
    public void setPanelXYForCurrentScaleForJSK(LineString ls, double floorPlanWidth, double floorPlanHeight, double floorPlanScale) {
    	ArrayList<Point> points = ls.getPoints();
    	
    	for (Point point : points) {
    		setPanelXYForCurrentScaleForJSK(point, floorPlanWidth, floorPlanHeight, floorPlanScale);
    	}
    }
    
    public void setPanelXYForCurrentScaleForJSK(Point p, double floorPlanWidth, double floorPlanHeight, double floorPlanScale) {
        double x = (p.getPanelRatioX() * floorPlanWidth * floorPlanScale);
        double y = (p.getPanelRatioY() * floorPlanHeight * floorPlanScale);

        p.setPanelX(x);
        p.setPanelY(y);
    }

    public void setPanelRatioXYForJSK(Point p, double floorPlanWidth, double floorPlanHeight, double floorPlanScale) {
        double x = p.getPanelX();
        double y = p.getPanelY();

        double ratioX = ((double) x / (floorPlanWidth * floorPlanScale));
        double ratioY = ((double) y / (floorPlanHeight * floorPlanScale));
        
        p.setPanelRatioX(ratioX);
        p.setPanelRatioY(ratioY);
    }
}
