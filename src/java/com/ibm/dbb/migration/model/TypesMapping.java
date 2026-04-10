/********************************************************************************
 * Licensed Materials - Property of IBM                                          *
 * (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
 *                                                                               *
 * Note to U.S. Government Users Restricted Rights:                              *
 * Use, duplication or disclosure restricted by GSA ADP Schedule                 *
 * Contract with IBM Corp.                                                       *
 ********************************************************************************/

package com.ibm.dbb.migration.model;

import java.util.List;
import java.util.Map;

/**
 * Types Mapping model class.
 * Represents the structure of the typesMapping YAML file.
 */
public class TypesMapping {
    private List<Map<String, String>> datasetMembers;

    public List<Map<String, String>> getDatasetMembers() {
        return datasetMembers;
    }

    public void setDatasetMembers(List<Map<String, String>> datasetMembers) {
        this.datasetMembers = datasetMembers;
    }
}

// Made with Bob
