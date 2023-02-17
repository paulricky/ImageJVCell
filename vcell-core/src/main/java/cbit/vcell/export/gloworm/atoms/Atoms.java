/*
 * Copyright (C) 1999-2011 University of Connecticut Health Center
 *
 * Licensed under the MIT License (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *  http://www.opensource.org/licenses/mit-license.php
 */

package cbit.vcell.export.gloworm.atoms;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
/**
 * This type was created in VisualAge.
 */
public abstract class Atoms implements AtomConstants {
	protected final static Logger lg = LogManager.getLogger(Atoms.class);

	protected int size;
		
/**
 * This method was created in VisualAge.
 * @param out java.io.DataOutputStream
 */
public abstract boolean writeData(DataOutputStream out);
}
