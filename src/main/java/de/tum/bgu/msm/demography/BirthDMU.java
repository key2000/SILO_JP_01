/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package de.tum.bgu.msm.demography;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import de.tum.bgu.msm.data.PersonType;

/**
 * Simulates birth of children 
 * Author: Rolf Moeckel, PB Albuquerque
 * Created on 28 December 2009 in Bocholt
 **/

public class BirthDMU {

    protected transient Logger logger = Logger.getLogger(BirthDMU.class);
    protected HashMap<String, Integer> methodIndexMap;

    // uec variables
    private int personType;
    private IndexValues dmuIndex;


    public BirthDMU() {
		dmuIndex = new IndexValues();
	}


    public void setType ( PersonType type ) {
    	this.personType = type.ordinal();
    }


    public IndexValues getDmuIndexValues() {
        return dmuIndex;
    }


    // DMU methods - define one of these for every @var in the mode choice control file.
	public int getPersonType() {
		return personType;
	}
}
