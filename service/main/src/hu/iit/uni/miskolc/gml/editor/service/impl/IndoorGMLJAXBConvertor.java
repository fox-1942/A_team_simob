package hu.iit.uni.miskolc.gml.editor.service.impl;

import com.vividsolutions.jts.geom.Envelope;
import edu.pnu.project.BoundaryType;
import edu.pnu.project.StateOnFloor;
import edu.pnu.project.TransitionOnFloor;
import net.opengis.gml.v_3_2_1.*;
import net.opengis.indoorgml.core.*;
import net.opengis.indoorgml.core.v_1_0.*;
import net.opengis.indoorgml.core.v_1_0.ObjectFactory;
import net.opengis.indoorgml.geometry.*;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndoorGMLJAXBConvertor {
    private int numberOfCell = 0;
    private int numberOfState = 0;
    private int numberOfState2 = 0;
    private int numberOfTransition = 0;
    private int numberOfTransition2 = 0;
    private boolean isLayer2 = false;


    private Envelope envelope;
    private double minZ = Double.NaN;
    private double maxZ = Double.NaN;

    private ObjectFactory IGMLFactory;
    private net.opengis.gml.v_3_2_1.ObjectFactory GMLFactory;

    private boolean is3DGeometry;
    private IndoorFeatures indoorFeatures;

    private Map<String, Object> idRegistry;

    private Map<CellSpaceBoundary, CellSpaceBoundary> boundary3DMap;

    private int orientableSurface_Label;

    public IndoorGMLJAXBConvertor(IndoorFeatures indoorFeatures, boolean is3DGeometry) {
        this.IGMLFactory = new net.opengis.indoorgml.core.v_1_0.ObjectFactory();
        this.GMLFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();

        this.indoorFeatures = indoorFeatures;
        this.is3DGeometry = is3DGeometry;

        idRegistry = new HashMap<String, Object>();
    }

    //code for jsk
    public IndoorGMLJAXBConvertor(IndoorFeatures indoorFeatures, boolean is3DGeometry, Map<CellSpaceBoundary, CellSpaceBoundary> boundary3DMap) {
        this.IGMLFactory = new net.opengis.indoorgml.core.v_1_0.ObjectFactory();
        this.GMLFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();

        this.indoorFeatures = indoorFeatures;
        this.is3DGeometry = is3DGeometry;

        idRegistry = new HashMap<String, Object>();

        this.boundary3DMap = boundary3DMap;

        orientableSurface_Label = 1;
    }

    public JAXBElement<IndoorFeaturesType> getJAXBElement() {
        IndoorFeaturesType indoorFeaturesType = createIndoorFeaturesType(null, indoorFeatures);
        JAXBElement<IndoorFeaturesType> je = IGMLFactory.createIndoorFeatures(indoorFeaturesType);

		/*
		System.out.println("Number of Cell : " + numberOfCell);
		System.out.println("Number of State : " + numberOfState);
		System.out.println("Number of Transition : " + numberOfTransition);
		System.out.println("Number of State2 : " + numberOfState2);
		System.out.println("Number of Transition2 : " + numberOfTransition2);
		*/

        return je;
    }

    private IndoorFeaturesType createIndoorFeaturesType(IndoorFeaturesType target, IndoorFeatures indoorFeatures) {
        if(target == null) {
            target = IGMLFactory.createIndoorFeaturesType();
        }
        // 준석햄 데이터를 위한 ID 생성
        String generatedID = generateGMLID(indoorFeatures);
        target.setId(generatedID);

        //target.setId(indoorFeatures.getGmlID());
        target.getName().add(createCodeType(null, indoorFeatures.getGmlID(), null));

        MultiLayeredGraphType multiLayeredGraphType = createMultiLayeredGraphType(null, indoorFeatures.getMultiLayeredGraph());
        target.setMultiLayeredGraph(multiLayeredGraphType);

        PrimalSpaceFeaturesType primalSpaceFeaturesType = createPrimalSpaceFeaturesType(null, indoorFeatures.getPrimalSpaceFeatures());
        PrimalSpaceFeaturesPropertyType primalSpaceFeaturesPropertyType = IGMLFactory.createPrimalSpaceFeaturesPropertyType();
        primalSpaceFeaturesPropertyType.setPrimalSpaceFeatures(primalSpaceFeaturesType);
        target.setPrimalSpaceFeatures(primalSpaceFeaturesPropertyType);

        idCheck(target);

        // BoundedBy
        BoundingShapeType boundingShapeType = createBoundedBy(null);
        target.setBoundedBy(boundingShapeType);

        return target;
    }

    private BoundingShapeType createBoundedBy(BoundingShapeType target) {
        if (target == null) {
            target = GMLFactory.createBoundingShapeType();
        }

        EnvelopeType envelopeType = GMLFactory.createEnvelopeType();

        DirectPositionType lowerCorner = GMLFactory.createDirectPositionType();
        lowerCorner.getValue().add(envelope.getMinX());
        lowerCorner.getValue().add(envelope.getMinY());
        lowerCorner.getValue().add(minZ);
        envelopeType.setLowerCorner(lowerCorner);

        DirectPositionType upperCorner = GMLFactory.createDirectPositionType();
        upperCorner.getValue().add(envelope.getMaxX());
        upperCorner.getValue().add(envelope.getMaxY());
        upperCorner.getValue().add(maxZ);
        envelopeType.setUpperCorner(upperCorner);

        envelopeType.setSrsDimension(BigInteger.valueOf(3));
        envelopeType.setSrsName("EPSG::4326"); // WGS84

        JAXBElement<EnvelopeType> jEnvelope = GMLFactory.createEnvelope(envelopeType);
        target.setEnvelope(jEnvelope);

        return target;
    }

    private PrimalSpaceFeaturesType createPrimalSpaceFeaturesType(PrimalSpaceFeaturesType target, PrimalSpaceFeatures primalSpaceFeatures) {
        if(target == null) {
            target = IGMLFactory.createPrimalSpaceFeaturesType();
        }

        String generatedID = generateGMLID(primalSpaceFeatures);
        target.setId(generatedID);
        //target.setId(primalSpaceFeatures.getGmlID());
        target.getName().add(createCodeType(null, primalSpaceFeatures.getGmlID(), null));

        ArrayList<CellSpaceOnFloor> cellSpaceOnFloors = primalSpaceFeatures.getCellSpaceOnFloors();
        for(CellSpaceOnFloor cellSpaceOnFloor : cellSpaceOnFloors) {
            target = createCellSpaceType(target, cellSpaceOnFloor);
        }

        ArrayList<CellSpaceBoundaryOnFloor> cellSpaceBoundaryOnFloors = primalSpaceFeatures.getCellSpaceBoundaryOnFloors();
        for(CellSpaceBoundaryOnFloor cellSpaceBoundaryOnFloor : cellSpaceBoundaryOnFloors) {
            target = createCellSpaceBoundaryType(target, cellSpaceBoundaryOnFloor);
        }

        idCheck(target);
        return target;
    }

    private PrimalSpaceFeaturesType createCellSpaceType(PrimalSpaceFeaturesType target, CellSpaceOnFloor cellSpaceOnFloor) {
        int count = 0;

        ArrayList<CellSpace> cellSpaceMember = cellSpaceOnFloor.getCellSpaceMember();
        for(CellSpace cellSpace : cellSpaceMember) {
            //cellSpaceMemberType = IGMLFactory.createCellSpaceMemberType();
            String description = cellSpace.getDescription("Usage");
            if (description == null || description.equals("")) {
                cellSpace.setDescription("Usage", "Room");
                description = cellSpace.getDescription("Usage");
            }

            setFloorDescription(cellSpace, cellSpaceOnFloor.getFloorProperty().getLevel());
            CellSpaceType cellSpaceType = createCellSpaceType(null, cellSpace);

            //cellSpaceMemberType.setCellSpace(cellSpaceType);
            FeaturePropertyType featurePropertyType = GMLFactory.createFeaturePropertyType();
            JAXBElement<CellSpaceType> jCellSpaceType = IGMLFactory.createCellSpace(cellSpaceType);
            featurePropertyType.setAbstractFeature(jCellSpaceType);
            target.getCellSpaceMember().add(featurePropertyType);
        }

        System.out.println("Floor : " + cellSpaceOnFloor.getFloorProperty().getLevel() + " stair count : " + count);

        return target;
    }

    private CellSpaceType createCellSpaceType(CellSpaceType target, CellSpace cellSpace) {
        numberOfCell++;

        if(target == null) {
            target = IGMLFactory.createCellSpaceType();
        }

        String generatedID = generateGMLID(cellSpace);
        target.setId(generatedID);
        //target.setId(cellSpace.getGmlID());
        target.getName().add(createCodeType(null, cellSpace.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, cellSpace.getDescription()));

        State duality = cellSpace.getDuality();
        if(duality != null) {
            StatePropertyType statePropertyType = IGMLFactory.createStatePropertyType();

            // 준석햄 데이터를 위한 ID 생성
            String generatedHref = generateGMLID(duality);
            statePropertyType.setHref("#" + generatedHref);
            //statePropertyType.setHref("#" + duality.getGmlID());
            target.setDuality(statePropertyType);
        }

        ArrayList<CellSpaceBoundary> partialBoundedBy = cellSpace.getPartialBoundedBy();
        for(CellSpaceBoundary cellSpaceBoundary : partialBoundedBy) {
            if(is3DGeometry && cellSpaceBoundary.getGeometry3D() == null) continue;
            else if(!is3DGeometry && cellSpaceBoundary.getGeometry2D() == null) continue;
            CellSpaceBoundaryPropertyType cellSpaceBoundaryPropertyType = IGMLFactory.createCellSpaceBoundaryPropertyType();

            String generatedHref = generateGMLID(cellSpaceBoundary);
            cellSpaceBoundaryPropertyType.setHref("#" + generatedHref);
            //cellSpaceBoundaryPropertyType.setHref("#" + cellSpaceBoundary.getGmlID());

            target.getPartialboundedBy().add(cellSpaceBoundaryPropertyType);
        }


        if(is3DGeometry) {
            // geometry3D solid
            SolidPropertyType solidPropertyType = createSolidPropertyType(null, cellSpace.getGeometry3D());

            target.setGeometry3D(solidPropertyType);
        } else {
            // geometry2D only polygon
            SurfacePropertyType surfacePropertyType = createSurfacePropertyType(null, cellSpace.getGeometry2D());
            target.setGeometry2D(surfacePropertyType);
        }
        // ExternalReference

        idCheck(target);
        return target;
    }

    private PrimalSpaceFeaturesType createCellSpaceBoundaryType(PrimalSpaceFeaturesType target, CellSpaceBoundaryOnFloor cellSpaceBoundaryOnFloor) {
        ArrayList<CellSpaceBoundary> cellSpaceBoundaryMember = cellSpaceBoundaryOnFloor.getCellSpaceBoundaryMember();
        for(CellSpaceBoundary cellSpaceBoundary : cellSpaceBoundaryMember) {
            //cellSpaceBoundaryMemberType = IGMLFactory.createCellSpaceBoundaryMemberType();
            if(is3DGeometry && cellSpaceBoundary.getGeometry3D() == null) continue;
            else if(!is3DGeometry && cellSpaceBoundary.getGeometry2D() == null) continue;

            setFloorDescription(cellSpaceBoundary, cellSpaceBoundaryOnFloor.getFloorProperty().getLevel());
            CellSpaceBoundaryType cellSpaceBoundaryType = createCellSpaceBoundaryType(null, cellSpaceBoundary);

            //cellSpaceBoundaryMemberType.setCellSpaceBoundary(cellSpaceBoundaryType);
            FeaturePropertyType featurePropertyType = GMLFactory.createFeaturePropertyType();
            JAXBElement<CellSpaceBoundaryType> jCellSpaceBoundaryType = IGMLFactory.createCellSpaceBoundary(cellSpaceBoundaryType);
            featurePropertyType.setAbstractFeature(jCellSpaceBoundaryType);
            target.getCellSpaceBoundaryMember().add(featurePropertyType);
        }

        return target;
    }

    private CellSpaceBoundaryType createCellSpaceBoundaryType(CellSpaceBoundaryType target, CellSpaceBoundary cellSpaceBoundary) {
        if (cellSpaceBoundary.getBoundaryType() == BoundaryType.Door) {
            cellSpaceBoundary.setDescription("Usage", "Door");

            if (cellSpaceBoundary.getDuality() == null) {
                System.out.println("****** " + cellSpaceBoundary.getGmlID() + " don't have duliaty !! ********");
            }
        }

        if(target == null) {
            target = IGMLFactory.createCellSpaceBoundaryType();
        }

        String generatedID = generateGMLID(cellSpaceBoundary);
        target.setId(generatedID);
        //target.setId(cellSpaceBoundary.getGmlID());
        target.getName().add(createCodeType(null, cellSpaceBoundary.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, cellSpaceBoundary.getDescription()));

        Transition duality = cellSpaceBoundary.getDuality();
        if(duality != null) {
            TransitionPropertyType transitionPropertyType = IGMLFactory.createTransitionPropertyType();

            String generatedHref = generateGMLID(duality);
            transitionPropertyType.setHref("#" + generatedHref);
            //transitionPropertyType.setHref("#" + duality.getGmlID());
            target.setDuality(transitionPropertyType);
        }

        if(is3DGeometry) {
            // geometry3D solid
            SurfacePropertyType surfacePropertyType = createSurfacePropertyType(null, cellSpaceBoundary.getGeometry3D());
            target.setGeometry3D(surfacePropertyType);
        } else {
            // geometry2D only polygon
            CurvePropertyType curvePropertyType = createCurvePropertyType(null, cellSpaceBoundary.getGeometry2D());
            target.setGeometry2D(curvePropertyType);
        }
        // ExternalReference

        idCheck(target);
        return target;
    }

    private MultiLayeredGraphType createMultiLayeredGraphType(MultiLayeredGraphType target, MultiLayeredGraph multiLayeredGraph) {
        if(target == null) {
            target = IGMLFactory.createMultiLayeredGraphType();
        }

        String generatedID = generateGMLID(multiLayeredGraph);
        target.setId(generatedID);
        //target.setId(multiLayeredGraph.getGmlID());
        target.getName().add(createCodeType(null, multiLayeredGraph.getName(), null));
        target.setDescription(createStringOrRefType(null, multiLayeredGraph.getDescription()));

        ArrayList<SpaceLayers> spaceLayersList = multiLayeredGraph.getSpaceLayers();
        for(SpaceLayers spaceLayers : spaceLayersList) {
            SpaceLayersType spaceLayersType = createSpaceLayersType(null, spaceLayers);
            target.getSpaceLayers().add(spaceLayersType);
        }

        ArrayList<InterEdges> interEdgesList = multiLayeredGraph.getInterEdges();
        for(InterEdges interEdges : interEdgesList) {
            InterEdgesType interEdgesType = createInterEdgesType(null, interEdges);
            if (interEdgesType != null) {
                target.getInterEdges().add(interEdgesType);
            }
        }

        return target;
    }

    private SpaceLayersType createSpaceLayersType(SpaceLayersType target, SpaceLayers spaceLayers) {
        if(target == null) {
            target = IGMLFactory.createSpaceLayersType();
        }

        String generatedID = generateGMLID(spaceLayers);
        target.setId(generatedID);
        //target.setId(spaceLayers.getGmlID());
        target.getName().add(createCodeType(null, spaceLayers.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, spaceLayers.getDescription()));

        ArrayList<SpaceLayer> spaceLayerList = spaceLayers.getSpaceLayerMember();
        for(SpaceLayer spaceLayer : spaceLayerList) {
            SpaceLayerMemberType spaceLayerMemberType = IGMLFactory.createSpaceLayerMemberType();
            SpaceLayerType spaceLayerType = createSpaceLayerType(null, spaceLayer);

            spaceLayerMemberType.setSpaceLayer(spaceLayerType);
            target.getSpaceLayerMember().add(spaceLayerMemberType);
        }

        idCheck(target);
        return target;
    }

    private SpaceLayerType createSpaceLayerType(SpaceLayerType target, SpaceLayer spaceLayer) {
        if (spaceLayer.getGmlID().equalsIgnoreCase("IS2")) {
            isLayer2 = true;
        }
        if(target == null) {
            target = IGMLFactory.createSpaceLayerType();
        }

        String generatedID = generateGMLID(spaceLayer);
        target.setId(generatedID);
        //target.setId(spaceLayer.getGmlID());
        target.getName().add(createCodeType(null, spaceLayer.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, spaceLayer.getDescription()));

        ArrayList<Nodes> nodesList = spaceLayer.getNodes();
        for(Nodes nodes : nodesList) {
            NodesType nodesType = createNodesType(null, nodes);

            target.getNodes().add(nodesType);
        }

        ArrayList<Edges> edgesList = spaceLayer.getEdges();
        for(Edges edges : edgesList) {
            EdgesType edgesType = createEdgesType(null, edges);

            target.getEdges().add(edgesType);
        }

        idCheck(target);
        return target;
    }

    private NodesType createNodesType(NodesType target, Nodes nodes) {
        if(target == null) {
            target = IGMLFactory.createNodesType();
        }

        String generatedID = generateGMLID(nodes);
        target.setId(generatedID);
        //target.setId(nodes.getGmlID());
        target.getName().add(createCodeType(null, nodes.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, nodes.getDescription()));

        ArrayList<StateOnFloor> stateOnFloorList = nodes.getStateOnFloors();
        for(StateOnFloor stateOnFloor : stateOnFloorList) {
            target = createStateType(target, stateOnFloor);
        }

        idCheck(target);
        return target;
    }

    private NodesType createStateType(NodesType target, StateOnFloor stateOnFloor) {
        ArrayList<State> stateList = stateOnFloor.getStateMember();
        for(State state : stateList) {
            setFloorDescription(state, stateOnFloor.getFloorProperty().getLevel());
            StateMemberType stateMemberType = IGMLFactory.createStateMemberType();
            StateType stateType = createStateType(null, state);

            stateMemberType.setState(stateType);
            target.getStateMember().add(stateMemberType);
        }

        return target;
    }

    private StateType createStateType(StateType target, State state) {
        numberOfState++;
        if (isLayer2) {
            numberOfState2++;
        }
        if(target == null) {
            target = IGMLFactory.createStateType();
        }

        String generatedID = generateGMLID(state);
        target.setId(generatedID);
        //target.setId(state.getGmlID());
        target.getName().add(createCodeType(null, state.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, state.getDescription()));

        ArrayList<Transition> connects = state.getTransitionReference();
        if(state.getTransitionReference().size() > 0) {
            for(Transition connect : connects) {
                TransitionPropertyType transitionPropertyType = IGMLFactory.createTransitionPropertyType();

                String generatedHref = generateGMLID(connect);
                transitionPropertyType.setHref("#" + generatedHref);
                //transitionPropertyType.setHref("#" + connect.getGmlID());

                target.getConnects().add(transitionPropertyType);
            }
        }

        CellSpace duality = state.getDuality();
        if(duality != null) {
            CellSpacePropertyType cellSpacePropertyType = IGMLFactory.createCellSpacePropertyType();

            String generatedHref = generateGMLID(duality);
            cellSpacePropertyType.setHref("#" + generatedHref);
            //cellSpacePropertyType.setHref("#" + duality.getGmlID());

            target.setDuality(cellSpacePropertyType);
        }

        PointPropertyType pointPropertyType = createPointPropertyType(null, state.getPosition());
        target.setGeometry(pointPropertyType);
		/*
		if(state.getName() != null) {
			CodeType codeType = GMLFactory.createCodeType();
			codeType.setValue(state.getName());

			stateType.getName().add(codeType);
		}*/

        idCheck(target);
        return target;
    }

    private EdgesType createEdgesType(EdgesType target, Edges edges) {
        if(target == null) {
            target = IGMLFactory.createEdgesType();
        }

        String generatedID = generateGMLID(edges);
        target.setId(generatedID);
        //target.setId(edges.getGmlID());
        target.getName().add(createCodeType(null, edges.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, edges.getDescription()));

        ArrayList<TransitionOnFloor> transitionOnFloorList = edges.getTransitionOnFloors();
        for(TransitionOnFloor transitionOnFloor : transitionOnFloorList) {
            target = createTransitionType(target, transitionOnFloor);
        }

        idCheck(target);
        return target;
    }

    private EdgesType createTransitionType(EdgesType target, TransitionOnFloor transitionOnFloor) {
        // TODO Auto-generated method stub
        ArrayList<Transition> transitionList = transitionOnFloor.getTransitionMember();

        for(Transition transition : transitionList) {
            setFloorDescription(transition, transitionOnFloor.getFloorProperty().getLevel());
            TransitionMemberType transitionMemberType = IGMLFactory.createTransitionMemberType();
            TransitionType transitionType = createTransitionType(null, transition);

            transitionMemberType.setTransition(transitionType);
            target.getTransitionMember().add(transitionMemberType);
        }

        return target;
    }

    private TransitionType createTransitionType(TransitionType target, Transition transition) {
        numberOfTransition++;
        if (isLayer2) {
            numberOfTransition2++;
        }
        if(target == null) {
            target = IGMLFactory.createTransitionType();
        }

        String generatedID = generateGMLID(transition);
        target.setId(generatedID);
        //target.setId(transition.getGmlID());
        target.getName().add(createCodeType(null, transition.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, transition.getDescription()));

        State[] states = transition.getStates();
        for(State state : states) {
            StatePropertyType statePropertyType = IGMLFactory.createStatePropertyType();

            String generatedHref = generateGMLID(state);
            statePropertyType.setHref("#" + generatedHref);
            //statePropertyType.setHref("#" + state.getGmlID());

            target.getConnects().add(statePropertyType);
        }

        CellSpaceBoundary duality = transition.getDuality();
        if(is3DGeometry && duality != null) {
            if (boundary3DMap.containsKey(duality)) {
                duality = boundary3DMap.get(duality);
            }
            CellSpaceBoundaryPropertyType cellSpaceBoundaryPropertyType = IGMLFactory.createCellSpaceBoundaryPropertyType();

            String generatedHref = generateGMLID(duality);
            cellSpaceBoundaryPropertyType.setHref("#" + generatedHref);
            //cellSpaceBoundaryPropertyType.setHref("#" + duality.getGmlID());

            target.setDuality(cellSpaceBoundaryPropertyType);
        }

        target.setWeight(transition.getWeight());

        CurvePropertyType curvePropertyType = createCurvePropertyType(null, transition.getPath());
        target.setGeometry(curvePropertyType);
		/*
		if(transition.getName() != null) {
			CodeType codeType = GMLFactory.createCodeType();
			codeType.setValue(transition.getName());

			transitionType.getName().add(codeType);
		}
		*/

        idCheck(target);
        return target;
    }

    private InterEdgesType createInterEdgesType(InterEdgesType target, InterEdges interEdges) {
        if (interEdges.getInterLayerConnectionMember().size() == 0) {
            return null;
        }

        if(target == null) {
            target = IGMLFactory.createInterEdgesType();
        }

        String generatedID = generateGMLID(interEdges);
        target.setId(generatedID);
        //target.setId(interEdges.getGmlID());
        target.getName().add(createCodeType(null, interEdges.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, interEdges.getDescription()));

        ArrayList<InterLayerConnection> interLayerConnectionList = interEdges.getInterLayerConnectionMember();
        for(InterLayerConnection interLayerConnection : interLayerConnectionList) {
            InterLayerConnectionMemberType interLayerConnectionMemberType = IGMLFactory.createInterLayerConnectionMemberType();
            InterLayerConnectionType interLayerConnectionType = createInterLayerConnectionType(null, interLayerConnection);

            interLayerConnectionMemberType.setInterLayerConnection(interLayerConnectionType);
            target.getInterLayerConnectionMember().add(interLayerConnectionMemberType);
        }

        idCheck(target);
        return target;
    }

    private InterLayerConnectionType createInterLayerConnectionType(InterLayerConnectionType target, InterLayerConnection interLayerConnection) {
        if(target == null) {
            target = IGMLFactory.createInterLayerConnectionType();
        }

        String generatedID = generateGMLID(interLayerConnection);
        target.setId(generatedID);
        //target.setId(interLayerConnection.getGmlID());
        target.getName().add(createCodeType(null, interLayerConnection.getGmlID(), null));
        target.setDescription(createStringOrRefType(null, interLayerConnection.getDescription()));
        target.setTypeOfTopoExpression(interLayerConnection.getTopology());
        target.setComment(interLayerConnection.getComment());

        State[] interConnects = interLayerConnection.getInterConnects();
        for(State state : interConnects) {
            StatePropertyType statePropertyType = IGMLFactory.createStatePropertyType();
            statePropertyType.setHref("#" + state.getGmlID());

            target.getInterConnects().add(statePropertyType);
        }

        SpaceLayer[] connectedLayers = interLayerConnection.getConnectedLayers();
        for(SpaceLayer spaceLayer : connectedLayers) {
            SpaceLayerPropertyType spaceLayerPropertyType = IGMLFactory.createSpaceLayerPropertyType();
            spaceLayerPropertyType.setHref("#" + spaceLayer.getGmlID());
            target.getConnectedLayers().add(spaceLayerPropertyType);
        }

        idCheck(target);
        return target;
    }

    private PointPropertyType createPointPropertyType(PointPropertyType target, Point point) {
        if(target == null) {
            target = GMLFactory.createPointPropertyType();
        }
        PointType pointType = GMLFactory.createPointType();

        //준석햄 데이터를 위한 ID 생성
        String generatedID = generateGMLID(point);
        pointType.setId(generatedID);
        //pointType.setId(point.getGMLID());
        pointType.getName().add(createCodeType(null, point.getGMLID(), null));

        DirectPositionType directPositionType = GMLFactory.createDirectPositionType();
        directPositionType.getValue().add(point.getRealX());
        directPositionType.getValue().add(point.getRealY());
        directPositionType.getValue().add(point.getZ());

        pointType.setPos(directPositionType);
        target.setPoint(pointType);

        idCheck(pointType);

        expandEnvelope(point.getRealX(), point.getRealY(), point.getZ());
        return target;
    }

    private CurvePropertyType createCurvePropertyType(CurvePropertyType target, LineString lineString) {
        if(target == null) {
            target = GMLFactory.createCurvePropertyType();
        }

        if(is3DGeometry && lineString.getxLinkGeometry() != null) {
            OrientableCurveType orientableCurveType = GMLFactory.createOrientableCurveType();
            CurvePropertyType baseCurvePropertyType = GMLFactory.createCurvePropertyType();

            baseCurvePropertyType.setHref("#" + lineString.getxLinkGeometry().getGMLID());
            orientableCurveType.setBaseCurve(baseCurvePropertyType);
            if(lineString.getIsReversed()) {
                orientableCurveType.setOrientation(SignType.VALUE_1);
            } else {
                orientableCurveType.setOrientation(SignType.VALUE_2);
            }
            JAXBElement<OrientableCurveType> jOrientableCurveType = GMLFactory.createOrientableCurve(orientableCurveType);
            target.setAbstractCurve(jOrientableCurveType);
        } else {
            LineStringType lineStringType = GMLFactory.createLineStringType();

            String generatedID = generateGMLID(lineString);
            lineStringType.setId(generatedID);
            //lineStringType.setId(lineString.getGMLID());

            //lineStringType.getName().add(createCodeType(lineString.getGMLID(), null));
            ArrayList<Point> points = lineString.getPoints();
            for(int i = 0; i < points.size(); i++) {
                Point point = points.get(i);

                DirectPositionType directPositionType = GMLFactory.createDirectPositionType();
                directPositionType.getValue().add(point.getRealX());
                directPositionType.getValue().add(point.getRealY());
                directPositionType.getValue().add(point.getZ());

                JAXBElement<DirectPositionType> jPosition = GMLFactory.createPos(directPositionType);
                lineStringType.getPosOrPointPropertyOrPointRep().add(jPosition);

                expandEnvelope(point.getRealX(), point.getRealY(), point.getZ());
            }

            JAXBElement<LineStringType> jAbstractCurve = GMLFactory.createLineString(lineStringType);
            target.setAbstractCurve(jAbstractCurve);

            idCheck(lineStringType);
        }

        return target;
    }

    private AbstractRingPropertyType createAbstractRingPropertyType(AbstractRingPropertyType target, LinearRing linearRing) {
        if(target == null) {
            target = GMLFactory.createAbstractRingPropertyType();
        }
        LinearRingType linearRingType = GMLFactory.createLinearRingType();
        ArrayList<Point> points = linearRing.getPoints();
        for(int i = 0; i < points.size(); i++) {
            Point point = points.get(i);

            DirectPositionType directPositonType = GMLFactory.createDirectPositionType();
            directPositonType.getValue().add(point.getRealX());
            directPositonType.getValue().add(point.getRealY());
            directPositonType.getValue().add(point.getZ());

            JAXBElement<DirectPositionType> jPosition = GMLFactory.createPos(directPositonType);
            linearRingType.getPosOrPointPropertyOrPointRep().add(jPosition);

            expandEnvelope(point.getRealX(), point.getRealY(), point.getZ());
        }

        JAXBElement<LinearRingType> jExteriorRing = GMLFactory.createLinearRing(linearRingType);
        target.setAbstractRing(jExteriorRing);

        return target;
    }

    private SurfacePropertyType createSurfacePropertyType(SurfacePropertyType target, Polygon polygon) {
        if(target == null) {
            target = GMLFactory.createSurfacePropertyType();
        }

        if(polygon.getxLinkGeometry() != null) {
            OrientableSurfaceType orientableSurfaceType = GMLFactory.createOrientableSurfaceType();
            orientableSurfaceType.setId("OrientableSurface" + orientableSurface_Label++);
            SurfacePropertyType baseSurfacePropertyType = GMLFactory.createSurfacePropertyType();

            baseSurfacePropertyType.setHref("#" + polygon.getxLinkGeometry().getGMLID());
            orientableSurfaceType.setBaseSurface(baseSurfacePropertyType);
            if(polygon.getIsReversed()) {
                orientableSurfaceType.setOrientation(SignType.VALUE_1);
            } else {
                orientableSurfaceType.setOrientation(SignType.VALUE_2);
            }
            JAXBElement<OrientableSurfaceType> jOrientableSurfaceType = GMLFactory.createOrientableSurface(orientableSurfaceType);
            target.setAbstractSurface(jOrientableSurfaceType);

            idCheck(orientableSurfaceType);
        } else {
            PolygonType polygonType = GMLFactory.createPolygonType();

            String generatedID = generateGMLID(polygon);
            polygonType.setId(generatedID);
            //polygonType.setId(polygon.getGMLID());
            polygonType.getName().add(createCodeType(null, polygon.getGMLID(), null));

            // exterior
            AbstractRingPropertyType abstractRingPropertyType = createAbstractRingPropertyType(null, polygon.getExteriorRing());
            polygonType.setExterior(abstractRingPropertyType);

            // interior
            ArrayList<LinearRing> interiorRings = polygon.getInteriorRing();
            for(LinearRing interiorRing : interiorRings) {
                abstractRingPropertyType = createAbstractRingPropertyType(null, interiorRing);
                polygonType.getInterior().add(abstractRingPropertyType);
            }
            JAXBElement<PolygonType> jPolygonType = GMLFactory.createPolygon(polygonType);
            target.setAbstractSurface(jPolygonType);

            idCheck(polygonType);
        }

        return target;
    }

    private ShellPropertyType createShellPropertyType(ShellPropertyType target, Shell shell) {
        if(target == null) {
            target = GMLFactory.createShellPropertyType();
        }
        ShellType shellType = GMLFactory.createShellType();
        ArrayList<Polygon> surfaceMember = shell.getSurfaceMember();
        for(Polygon polygon : surfaceMember) {
            SurfacePropertyType surfacePropertyType = createSurfacePropertyType(null, polygon);
            shellType.getSurfaceMember().add(surfacePropertyType);
        }
		/*Polygon polygon = surfaceMember.get(surfaceMember.size() - 1);
		surfacePropertyType = GMLFactory.createSurfacePropertyType();
                visit(polygon);
                shellType.getSurfaceMember().add(surfacePropertyType);*/

        target.setShell(shellType);
        return target;
    }

    private SolidPropertyType createSolidPropertyType(SolidPropertyType target, Solid solid) {
        // TODO Auto-generated method stub
        if(target == null) {
            target = GMLFactory.createSolidPropertyType();
        }
        SolidType solidType = GMLFactory.createSolidType();

        String generatedID = generateGMLID(solid);
        solidType.setId(generatedID);
        //solidType.setId(solid.getGMLID());
        //solidType.getName().add(createCodeType(solid.getGMLID(), null));

        // exteior
        ShellPropertyType shellPropertyType = createShellPropertyType(null, solid.getExteriorShell());
        solidType.setExterior(shellPropertyType);

        // interior


        JAXBElement<SolidType> jSolidType = GMLFactory.createSolid(solidType);
        target.setAbstractSolid(jSolidType);

        idCheck(solidType);
        return target;
    }

    private CodeType createCodeType(CodeType target, String name, String codeSpace) {
        if(name == null && codeSpace == null)
            return null;

        if(target == null) {
            target = GMLFactory.createCodeType();
        }

        if(name != null) {
            target.setValue(name);
        }
        if(codeSpace != null) {
            target.setCodeSpace(codeSpace);
        }

        return target;
    }

    private StringOrRefType createStringOrRefType(StringOrRefType target, String value) {
        if(value == null || value.equals(""))
            return null;

        if(target == null) {
            target= GMLFactory.createStringOrRefType();
        }

        target.setValue(value);
        return target;
    }

    private void setFloorDescription(AbstractFeature target, String floor) {
        String[] splits = floor.split("_");

        if (splits.length < 2) {
            return;
        }
        String section = splits[0];

        floor = splits[1];
        if (floor.contains("F")) {
            floor = floor.replace("F", "");
        }
        int intFloor;
        if (floor.startsWith("B")) {
            floor = floor.replace("B", "");
            intFloor = -1 * Integer.parseInt(floor);
        } else {
            intFloor = Integer.parseInt(floor);
        }

        target.setDescription("Section", section);
        target.setDescription("Floor", String.valueOf(intFloor));
    }

    private void idCheck(AbstractFeatureType target) {
        if (idRegistry.containsKey(target.getId())) {
            System.out.println("** Chekcer : " + target.getId() + " found");
        } else {
            idRegistry.put(target.getId(), target);
        }
    }

    private void idCheck(AbstractGMLType target) {
        if (idRegistry.containsKey(target.getId())) {
            System.out.println("** Chekcer : " + target.getId() + " found");
        } else {
            idRegistry.put(target.getId(), target);
        }
    }

    private String generateGMLID(AbstractFeature target) {
        return target.getGmlID();
		/*
		String origin = target.getGmlID();
		String intValue = origin.replaceAll("[^0-9]", "");
		String typeCode = getIDTypeCode(target);
		StringBuffer sb = new StringBuffer();
		sb.append(intValue);
		sb.append(typeCode);

		String generated = sb.toString();
		return generated;
		*/
    }

    private String generateGMLID(AbstractGeometry target) {
        return target.getGMLID();
		/*
		String origin = target.getGMLID();
		String intValue = origin.replaceAll("[^0-9]", "");
		String typeCode = getIDTypeCode(target);
		StringBuffer sb = new StringBuffer();
		sb.append(intValue);
		sb.append(typeCode);

		String generated = sb.toString();
		return generated;
		*/
    }

    private String getIDTypeCode(Object object) {
        String code = null;

        if (object instanceof IndoorFeatures) {
            code = "01";
        } else if (object instanceof PrimalSpaceFeatures) {
            code = "02";
        } else if (object instanceof CellSpace) {
            code = "03";
        } else if (object instanceof CellSpaceBoundary) {
            code = "04";
        } else if (object instanceof MultiLayeredGraph) {
            code = "05";
        } else if (object instanceof SpaceLayers) {
            code = "06";
        } else if (object instanceof SpaceLayer) {
            code = "07";
        } else if (object instanceof Nodes) {
            code = "08";
        } else if (object instanceof State) {
            code = "09";
        } else if (object instanceof Edges) {
            code = "10";
        } else if (object instanceof Transition) {
            code = "11";
        } else if (object instanceof InterEdges) {
            code = "12";
        } else if (object instanceof InterLayerConnection) {
            code = "13";
        } else if (object instanceof Point) {
            code = "20";
        } else if (object instanceof LineString) {
            code = "21";
        } else if (object instanceof Polygon) {
            code = "22";
        } else if (object instanceof Solid) {
            code = "23";
        }

        return code;
    }

    private void expandEnvelope(double x, double y, double z) {
        if (envelope == null) {
            envelope = new Envelope();
            minZ = z;
            maxZ = z;
        }

        envelope.expandToInclude(x, y);
        if (minZ > z) {
            minZ = z;
        }
        if (maxZ < z) {
            maxZ = z;
        }
    }
}
