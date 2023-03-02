/*
 * Copyright (C) 1999-2011 University of Connecticut Health Center
 *
 * Licensed under the MIT License (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *  http://www.opensource.org/licenses/mit-license.php
 */

package cbit.vcell.mapping;
import java.beans.PropertyVetoException;

import cbit.vcell.mapping.ParameterContext.LocalParameter;
import cbit.vcell.model.Parameter;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionBindingException;
import cbit.vcell.units.VCUnitDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Insert the type's description here.
 * Creation date: (4/8/2002 11:45:12 AM)
 * @author: Anuradha Lakshminarayana
 */
public class VoltageClampStimulus extends ElectricalStimulus {
	private final static Logger lg = LogManager.getLogger(VoltageClampStimulus.class);

	public VoltageClampStimulus(Electrode argElectrode, String argName, Expression argVoltExpr, SimulationContext argSimulationContext) {
	super(argElectrode, argName, argSimulationContext);

	try {
		argVoltExpr.bindExpression(parameterContext);
	}catch (ExpressionBindingException e){
		lg.error(e);
		//throw new RuntimeException(e.getMessage());
	}
	LocalParameter[] localParameters = new LocalParameter[1];
	VCUnitDefinition voltageUnit = argSimulationContext.getModel().getUnitSystem().getVoltageUnit();
	localParameters[0] = parameterContext.new LocalParameter(
			ElectricalStimulusParameterType.Voltage.defaultName, argVoltExpr, 
			ElectricalStimulusParameterType.Voltage, voltageUnit, 
			"applied voltage");
	
	try {
		parameterContext.setLocalParameters(localParameters);
	} catch (PropertyVetoException | ExpressionBindingException e) {
		throw new RuntimeException(e.getMessage(), e);
	}
}


public VoltageClampStimulus(VoltageClampStimulus otherStimulus, SimulationContext argSimulationContext) {
	super(otherStimulus, argSimulationContext);
}


/**
 * Checks for internal representation of objects, not keys from database
 * @return boolean
 * @param obj java.lang.Object
 */
public boolean compareEqual(org.vcell.util.Matchable obj) {
	if (obj instanceof VoltageClampStimulus){
		if (!super.compareEqual0(obj)){
			return false;
		}
		
		return true;
	}else{
		return false;
	}

}

public LocalParameter getVoltageParameter() {
	return parameterContext.getLocalParameterFromRole(ElectricalStimulusParameterType.Voltage);
}


@Override
public Parameter getProtocolParameter() {
	return getVoltageParameter();
}


}
