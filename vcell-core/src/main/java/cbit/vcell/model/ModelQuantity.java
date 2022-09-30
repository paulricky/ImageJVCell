/*
 * Copyright (C) 1999-2011 University of Connecticut Health Center
 *
 * Licensed under the MIT License (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *  http://www.opensource.org/licenses/mit-license.php
 */

package cbit.vcell.model;
import org.vcell.util.Compare;

import cbit.vcell.parser.Expression;
import cbit.vcell.parser.NameScope;
import cbit.vcell.units.VCUnitDefinition;
import org.vcell.util.Matchable;
import org.vcell.util.Relatable;
import org.vcell.util.RelationVisitor;

import java.io.Serializable;

/**
 * Insert the type's description here.
 * Creation date: (2/20/2002 4:16:31 PM)
 * @author: Jim Schaff
 */
public abstract class ModelQuantity implements EditableSymbolTableEntry, Serializable, Matchable, Relatable {
	private java.lang.String fieldName = new String();
	private transient java.beans.VetoableChangeSupport vetoPropertyChange;
	private transient java.beans.PropertyChangeSupport propertyChange;

/**
 * MembraneVoltage constructor comment.
 */
public ModelQuantity(String name) {
	super();
	fieldName = name;
}


/**
 * The addPropertyChangeListener method was generated to support the propertyChange field.
 */
public final synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
	getPropertyChange().addPropertyChangeListener(listener);
}


/**
 * The addPropertyChangeListener method was generated to support the propertyChange field.
 */
public final synchronized void addPropertyChangeListener(java.lang.String propertyName, java.beans.PropertyChangeListener listener) {
	getPropertyChange().addPropertyChangeListener(propertyName, listener);
}


/**
 * The addVetoableChangeListener method was generated to support the vetoPropertyChange field.
 */
public final synchronized void addVetoableChangeListener(java.beans.VetoableChangeListener listener) {
	getVetoPropertyChange().addVetoableChangeListener(listener);
}


/**
 * The addVetoableChangeListener method was generated to support the vetoPropertyChange field.
 */
public final synchronized void addVetoableChangeListener(java.lang.String propertyName, java.beans.VetoableChangeListener listener) {
	getVetoPropertyChange().addVetoableChangeListener(propertyName, listener);
}


	public final boolean compareEqual(org.vcell.util.Matchable obj){
		if (obj==null){
			return false;
		}
		if (obj==this){
			return true;
		}
		if (obj.getClass().equals(getClass())){
			ModelQuantity modelQuantity = (ModelQuantity)obj;
			if (!modelQuantity.getName().equals(getName())){
				return false;
			}
			if (!Compare.isEqual(modelQuantity.getUnitDefinition(),getUnitDefinition())){
				return false;
			}
		}
		return true;
	}

	@Override
	public final boolean relate(Relatable obj, RelationVisitor rv){
		if (obj==null){
			return false;
		}
		if (obj==this){
			return true;
		}
		if (obj.getClass().equals(getClass())){
			ModelQuantity modelQuantity = (ModelQuantity)obj;
			if (!rv.relate(modelQuantity.getName(),getName())){
				return false;
			}
			if (!rv.relate(modelQuantity.getUnitDefinition(),getUnitDefinition())){
				return false;
			}
		}
		return true;
	}

	/**
 * The firePropertyChange method was generated to support the propertyChange field.
 */
public final void firePropertyChange(java.lang.String propertyName, java.lang.Object oldValue, java.lang.Object newValue) {
	getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
}


/**
 * The fireVetoableChange method was generated to support the vetoPropertyChange field.
 */
public final void fireVetoableChange(java.lang.String propertyName, java.lang.Object oldValue, java.lang.Object newValue) throws java.beans.PropertyVetoException {
	getVetoPropertyChange().fireVetoableChange(propertyName, oldValue, newValue);
}


/**
 * This method was created in VisualAge.
 * @return double
 */
public final double getConstantValue() throws cbit.vcell.parser.ExpressionException {
	throw new cbit.vcell.parser.ExpressionException("getConstantValue(): not supported for "+getClass().getName());
}


/**
 * This method was created by a SmartGuide.
 * @return boolean
 * @exception java.lang.Exception The exception description.
 */
public final Expression getExpression() {
	return null;
}

public final void setExpression(Expression exp){
	throw new RuntimeException("cannot set expression on '"+getName()+"'");
}

/**
 * This method was created in VisualAge.
 * @return int
 */
public final int getIndex() {
	return -1;
}


/**
 * Gets the name property (java.lang.String) value.
 * @return The name property value.
 * @see #setName
 */
public final java.lang.String getName() {
	return fieldName;
}


/**
 * Insert the method's description here.
 * Creation date: (7/31/2003 10:28:46 AM)
 * @return cbit.vcell.parser.NameScope
 */
public abstract NameScope getNameScope();


/**
 * Accessor for the propertyChange field.
 */
protected java.beans.PropertyChangeSupport getPropertyChange() {
	if (propertyChange == null) {
		propertyChange = new java.beans.PropertyChangeSupport(this);
	};
	return propertyChange;
}


/**
 * Insert the method's description here.
 * Creation date: (3/31/2004 12:09:04 PM)
 * @return cbit.vcell.units.VCUnitDefinition
 */
public abstract VCUnitDefinition getUnitDefinition();

/**
 * Accessor for the vetoPropertyChange field.
 */
protected java.beans.VetoableChangeSupport getVetoPropertyChange() {
	if (vetoPropertyChange == null) {
		vetoPropertyChange = new java.beans.VetoableChangeSupport(this);
	};
	return vetoPropertyChange;
}


/**
 * The hasListeners method was generated to support the propertyChange field.
 */
public final synchronized boolean hasListeners(java.lang.String propertyName) {
	return getPropertyChange().hasListeners(propertyName);
}


/**
 * This method was created by a SmartGuide.
 * @return boolean
 * @exception java.lang.Exception The exception description.
 */
public final boolean isConstant() throws cbit.vcell.parser.ExpressionException {
	return false;
}

public boolean isNameEditable() {
	return true;
}

public final boolean isExpressionEditable() {
	return false;
}

public boolean isDescriptionEditable() {
	return false;
}

public void setDescription(String description){
	throw new RuntimeException("description is not editable");
}

public String getDescription(){
	return null;
}

/**
 * The removePropertyChangeListener method was generated to support the propertyChange field.
 */
public final synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
	getPropertyChange().removePropertyChangeListener(listener);
}


/**
 * The removePropertyChangeListener method was generated to support the propertyChange field.
 */
public final synchronized void removePropertyChangeListener(java.lang.String propertyName, java.beans.PropertyChangeListener listener) {
	getPropertyChange().removePropertyChangeListener(propertyName, listener);
}


/**
 * The removeVetoableChangeListener method was generated to support the vetoPropertyChange field.
 */
public final synchronized void removeVetoableChangeListener(java.beans.VetoableChangeListener listener) {
	getVetoPropertyChange().removeVetoableChangeListener(listener);
}


/**
 * The removeVetoableChangeListener method was generated to support the vetoPropertyChange field.
 */
public final synchronized void removeVetoableChangeListener(java.lang.String propertyName, java.beans.VetoableChangeListener listener) {
	getVetoPropertyChange().removeVetoableChangeListener(propertyName, listener);
}


/**
 * Sets the name property (java.lang.String) value.
 * @param name The new value for the property.
 * @exception java.beans.PropertyVetoException The exception description.
 * @see #getName
 */
public final void setName(java.lang.String name) throws java.beans.PropertyVetoException {
	String oldValue = fieldName;
	fireVetoableChange("name", oldValue, name);
	fieldName = name;
	firePropertyChange("name", oldValue, name);
}
}
