package cbit.vcell.mapping.spatial.processes;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import org.vcell.util.Compare;
import org.vcell.util.Issue;
import org.vcell.util.Issue.IssueCategory;
import org.vcell.util.IssueContext;
import org.vcell.util.Matchable;

import cbit.vcell.mapping.ParameterContext.LocalParameter;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.mapping.spatial.PointObject;
import cbit.vcell.mapping.spatial.SpatialObject;
import cbit.vcell.mapping.spatial.SpatialObject.QuantityCategory;
import cbit.vcell.mapping.spatial.SpatialObject.QuantityComponent;
import cbit.vcell.mapping.spatial.SpatialObject.SpatialQuantity;
import cbit.vcell.model.ModelUnitSystem;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionBindingException;
import cbit.vcell.units.VCUnitDefinition;

public class PointKinematics extends SpatialProcess {
	private PointObject pointObject = null;

	public PointKinematics(String name, SimulationContext simContext) {
		super(name, simContext);

		ModelUnitSystem units = simContext.getModel().getUnitSystem();
		VCUnitDefinition velocityUnit = units.getLengthUnit().divideBy(units.getTimeUnit());
		VCUnitDefinition lengthUnit = units.getLengthUnit();
		LocalParameter initX = createNewParameter("initialPosX", SpatialProcessParameterType.PointInitialPositionX, new Expression(0.0), lengthUnit);
		LocalParameter initY = createNewParameter("initialPosY", SpatialProcessParameterType.PointInitialPositionY, new Expression(0.0), lengthUnit);
		LocalParameter initZ = createNewParameter("initialPosZ", SpatialProcessParameterType.PointInitialPositionZ, new Expression(0.0), lengthUnit);
		LocalParameter velX = createNewParameter("velocityX", SpatialProcessParameterType.PointVelocityX, new Expression(0.0), velocityUnit);
		LocalParameter velY = createNewParameter("velocityY", SpatialProcessParameterType.PointVelocityY, new Expression(0.0), velocityUnit);
		LocalParameter velZ = createNewParameter("velocityZ", SpatialProcessParameterType.PointVelocityZ, new Expression(0.0), velocityUnit);
		try {
			setParameters(new LocalParameter[] { initX, initY, initZ, velX, velY, velZ });
		} catch (ExpressionBindingException | PropertyVetoException e) {
			throw new RuntimeException("failed to create parameters: "+e.getMessage(),e);
		}
	}

	public PointKinematics(PointKinematics argPointKinematics, SimulationContext argSimContext) {
		super(argPointKinematics, argSimContext);
		this.pointObject = (PointObject)argSimContext.getSpatialObject(argPointKinematics.getPointObject().getName());
	}
	
	@Override
	public List<SpatialQuantity> getReferencedSpatialQuantities() {
		ArrayList<SpatialQuantity> spatialQuantities = new ArrayList<SpatialQuantity>();
		if (pointObject!=null){
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointPosition,QuantityComponent.X));
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointPosition,QuantityComponent.Y));
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointPosition,QuantityComponent.Z));
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointVelocity,QuantityComponent.X));
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointVelocity,QuantityComponent.Y));
			spatialQuantities.add(pointObject.getSpatialQuantity(QuantityCategory.PointVelocity,QuantityComponent.Z));
		}
		return spatialQuantities;
	}
	
	public void setPointObject(PointObject pointObject){
		this.pointObject = pointObject;
	}
	
	public PointObject getPointObject(){
		return this.pointObject;
	}

	@Override
	public boolean compareEqual(Matchable obj) {
		if (obj instanceof PointKinematics){
			PointKinematics other = (PointKinematics)obj;
			if (!compareEqual0(other)){
				return false;
			}
			if (!Compare.isEqualOrNull(pointObject, other.pointObject)){
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return "Point Kinematics (initial position, velocity)";
	}

	@Override
	public List<SpatialObject> getSpatialObjects() {
		ArrayList<SpatialObject> spatialObjects = new ArrayList<SpatialObject>();
		spatialObjects.add(pointObject);
		return spatialObjects;
	}

	@Override
	public void gatherIssues(IssueContext issueContext, List<Issue> issueList) {
		if (simulationContext!=null && pointObject!=null){
			if (simulationContext.getSpatialObject(pointObject.getName()) != pointObject){
				issueList.add(new Issue(this, issueContext, IssueCategory.Identifiers, "Point Kinematics '"+getName()+"' refers to missing PointObject '"+pointObject.getName()+" (see Spatial Objects)", Issue.Severity.ERROR));
			}
			if (!pointObject.isQuantityCategoryEnabled(QuantityCategory.PointPosition)){
				issueList.add(new Issue(pointObject, issueContext, IssueCategory.Identifiers,"Point Kinematics '"+getName()+"' refers to disabled quantity '"+QuantityCategory.PointPosition.description+"', please enable it.", Issue.Severity.ERROR));
			}
			if (!pointObject.isQuantityCategoryEnabled(QuantityCategory.PointVelocity)){
				issueList.add(new Issue(pointObject, issueContext, IssueCategory.Identifiers, "Point Kinematics '"+getName()+"' refers to disabled quantity '"+QuantityCategory.PointVelocity.description+"', please enable it.", Issue.Severity.ERROR));
			}
		}
	}

}
