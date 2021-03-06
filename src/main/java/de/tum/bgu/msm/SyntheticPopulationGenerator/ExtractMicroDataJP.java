package de.tum.bgu.msm.SyntheticPopulationGenerator;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.SiloUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * Reads the micro data as an input for the synthetic population
 * @author Ana Moreno (TUM) and Masanobu Kii (Kagawa University)
 * Created on August 2, 2017 in Munich
 *
 */


public class ExtractMicroDataJP {

    private ResourceBundle rb;

    //Routes of the input data
    protected static final String PROPERTIES_MICRODATA_JP                 = "micro.data";

    //Attributes at the person and household level
    protected static final String PROPERTIES_VARIABLES                    = "attributes.micro.data";
    protected static final String PROPERTIES_EXCEPTION_MICRODATA          = "microData.exceptions";

    //Conversion tables from microdata categories to control total categories
    protected static final String PROPERTIES_HOUSEHOLD_ATTRIBUTES         = "attributes.control.total";
    protected static final String PROPERTIES_HOUSEHOLD_SIZES              = "household.size.brackets";
    protected static final String PROPERTIES_MICRO_DATA_AGES              = "age.brackets";
    protected static final String PROPERTIES_MICRO_DATA_GENDER            = "gender.brackets";
    protected static final String PROPERTIES_MICRO_DATA_OCCUPATION        = "occupation.brackets";
    protected static final String PROPERTIES_MICRO_DATA_DWELLING_USE      = "use.brackets";
    protected static final String PROPERTIES_MICRO_DATA_TYPE              = "type.brackets";
    protected static final String PROPERTIES_DD_USE_DICTIONARY            = "dd.use.dictionary";
    protected static final String PROPERTIES_DD_TYPE_DICTIONARY           = "dd.type.dictionary";
    protected static final String PROPERTIES_PP_JOB_DICTIONARY            = "pp.jobs.dictionary";

    protected TableDataSet microHouseholds;
    protected TableDataSet microPersons;
    protected TableDataSet microDwellings;
    protected TableDataSet frequencyMatrix;


    protected int[] householdSizes;
    protected int[] ageBracketsPerson;
    protected int[] genderBrackets;
    protected String[] occupationBrackets;
    protected String[] usageBracketsDwelling;
    private String[] typeBracketsDwelling;

    protected String[] attributesMunicipality;


    static Logger logger = Logger.getLogger(SyntheticPopDe.class);
    private String[] attributesControlTotal;
    private TableDataSet variables;
    private HashMap<String, String[]> attributesMicroData;
    private TableDataSet ddUseDictionary;
    private TableDataSet ddTypeDictionary;
    private TableDataSet ppJobDictionary;
    private HashMap<String, String> attributesDictionary;


    public ExtractMicroDataJP(ResourceBundle rb) {
        // Constructor
        this.rb = rb;
    }

    public TableDataSet getFrequencyMatrix() {
        return frequencyMatrix;
    }

    public TableDataSet getMicroHouseholds() {
        return microHouseholds;
    }

    public TableDataSet getMicroPersons() {
        return microPersons;
    }

    public TableDataSet getMicroDwellings() {
        return microDwellings;
    }

    public void run(){
        //method to create the synthetic population
        logger.info("   Starting to create the synthetic population.");
        long startTime = System.nanoTime();
        setInputData();
        setAttributesToCopyFromMicroData();
        readCSVMicroData();
        createFrequencyMatrix();
        long estimatedTime = System.nanoTime() - startTime;
        logger.info("   Finished creating the synthetic population. Elapsed time: " + estimatedTime);
    }


    private void setInputData() {
        //method with all the inputs that are required to read externally for this class

        //To exclude from the microData some records
        //exceptions = SiloUtil.readCSVfile(rb.getString(PROPERTIES_EXCEPTION_MICRODATA));

        //Brackets to define attributesControlTotal for IPU
        householdSizes = ResourceUtil.getIntegerArray(rb,PROPERTIES_HOUSEHOLD_SIZES);
        ageBracketsPerson = ResourceUtil.getIntegerArray(rb, PROPERTIES_MICRO_DATA_AGES);
        genderBrackets = ResourceUtil.getIntegerArray(rb, PROPERTIES_MICRO_DATA_GENDER);
        occupationBrackets = ResourceUtil.getArray(rb,PROPERTIES_MICRO_DATA_OCCUPATION);
        usageBracketsDwelling = ResourceUtil.getArray(rb,PROPERTIES_MICRO_DATA_DWELLING_USE);
        typeBracketsDwelling = ResourceUtil.getArray(rb,PROPERTIES_MICRO_DATA_TYPE);

        //Attributes list
        attributesControlTotal = ResourceUtil.getArray(rb, PROPERTIES_HOUSEHOLD_ATTRIBUTES);
        variables = SiloUtil.readCSVfile(rb.getString(PROPERTIES_VARIABLES));
        variables.buildStringIndex(variables.getColumnPosition("VariableNameMicroData"));

        //Dictionary from the microdata to the control total
        ddUseDictionary = SiloUtil.readCSVfile(rb.getString(PROPERTIES_DD_USE_DICTIONARY));
        ddUseDictionary.buildIndex(ddUseDictionary.getColumnPosition("microDataLabel"));
        ddTypeDictionary = SiloUtil.readCSVfile(rb.getString(PROPERTIES_DD_TYPE_DICTIONARY));
        ddTypeDictionary.buildIndex(ddTypeDictionary.getColumnPosition("microDataLabel"));
        ppJobDictionary = SiloUtil.readCSVfile(rb.getString(PROPERTIES_PP_JOB_DICTIONARY));
        ppJobDictionary.buildIndex(ppJobDictionary.getColumnPosition("microDataLabel"));
    }


    private void setAttributesToCopyFromMicroData() {
        //method to set the attributesControlTotal to read

        attributesMicroData = new HashMap<>();
        for (int i = 1; i <= variables.getRowCount(); i++){
            String key = variables.getStringValueAt(i,"Type");
            String value = variables.getStringValueAt(i,"VariableNameMicroData");
            if (attributesMicroData.containsKey(key)){
                String[] previous = attributesMicroData.get(key);
                previous = SiloUtil.expandArrayByOneElement(previous, value);
                attributesMicroData.put(key,previous);
            } else {
                String[] previous = new String[1];
                previous[0] = value;
                attributesMicroData.put(key,previous);
            }
        }
    }


    private void readCSVMicroData() {

        TableDataSet microData = SiloUtil.readCSVfile(rb.getString(PROPERTIES_MICRODATA_JP));
        int hhCount = 0;
        int personCount = 0;
        int previoushhID = 0;
        for (int i = 1; i <=microData.getRowCount(); i++){
            if ((int) microData.getValueAt(i,"H_Code") != previoushhID){
                hhCount++;
                previoushhID = (int) microData.getValueAt(i,"H_Code");
            }
        }
        initializeMicroData(hhCount, microData.getRowCount());
        String ppFileName = ("input/testing/output/microPersons.csv");
        SiloUtil.writeTableDataSet(microPersons, ppFileName);
        String hhFileName = ("input/testing/output/microHouseholds.csv");
        SiloUtil.writeTableDataSet(microHouseholds, hhFileName);
        String ddFileName = ("input/testing/output/microDwellings.csv");
        SiloUtil.writeTableDataSet(microDwellings, ddFileName);
        SiloUtil.writeTableDataSet(microData,"input/testing/output/microDATA.csv");
        hhCount = 0;
        personCount = 0;
        for (int i = 1; i <=microData.getRowCount();i++){
            int householdNumber = (int) microData.getValueAt(i,"H_Code");
            if ( householdNumber != previoushhID){
                hhCount++;
                personCount++;
                microHouseholds.setValueAt(hhCount,"id",hhCount);
                microHouseholds.setValueAt(hhCount,"H_Code",householdNumber);
                microHouseholds.setValueAt(hhCount,"firstPerson",personCount);
                for (int j = 0; j < attributesMicroData.get("Household").length; j++){
                    int value = (int) microData.getValueAt(i,attributesMicroData.get("Household")[j]);
                    microHouseholds.setValueAt(hhCount,attributesMicroData.get("Household")[j],value);
                }
                microDwellings.setValueAt(hhCount,"id",hhCount);
                for (int j = 0; j < attributesMicroData.get("Dwelling").length; j++){
                    int value = (int) microData.getValueAt(i,attributesMicroData.get("Dwelling")[j]);
                    microDwellings.setValueAt(hhCount,attributesMicroData.get("Dwelling")[j],value);
                }
                microPersons.setValueAt(personCount,"id",personCount);
                microPersons.setValueAt(personCount,"idHH",hhCount);
                microPersons.setValueAt(personCount,"H_Code",householdNumber);
                for (int j = 0; j < attributesMicroData.get("Person").length; j++){
                    int value = (int) microData.getValueAt(personCount,attributesMicroData.get("Person")[j]);
                    microPersons.setValueAt(personCount,attributesMicroData.get("Person")[j],value);
                }
                previoushhID = householdNumber;
            } else {
                personCount++;
                microPersons.setValueAt(personCount,"id",personCount);
                microPersons.setValueAt(personCount,"idHH",hhCount);
                microPersons.setValueAt(personCount,"H_Code",householdNumber);
                for (int j = 0; j < attributesMicroData.get("Person").length; j++){
                    int value = (int) microData.getValueAt(personCount,attributesMicroData.get("Person")[j]);
                    microPersons.setValueAt(personCount,attributesMicroData.get("Person")[j],value);
                }
            }
        }
        ppFileName = ("input/testing/output/microPersons.csv");
        SiloUtil.writeTableDataSet(microPersons, ppFileName);
        hhFileName = ("input/testing/output/microHouseholds.csv");
        SiloUtil.writeTableDataSet(microHouseholds, hhFileName);
        ddFileName = ("input/testing/output/microDwellings.csv");
        SiloUtil.writeTableDataSet(microDwellings, ddFileName);
    }


    private void createFrequencyMatrix(){
        //create the frequency matrix with all the attributesControlTotal aggregated at the household level
        logger.info("   Starting to create the frequency matrix");

        //Read the attributesControlTotal to match and initialize frequency matrix
        initializeAttributesMunicipality();
        attributesMunicipality = frequencyMatrix.getColumnLabels();

       //Update the frequency matrix with the microdata
        for (int i = 1; i <= frequencyMatrix.getRowCount(); i++){
            //checkContainsAndUpdate(attributesMunicipality[i],);
            frequencyMatrix.setValueAt(i,"hhTotal",1);
            String attributeMicroData = attributesDictionary.get("nHH_");
            int hhSize = (int) microHouseholds.getValueAt(i,attributeMicroData);
            updateHhSize(hhSize, i);
            attributeMicroData = attributesDictionary.get("H_");
            updateDdUse((int) microDwellings.getValueAt(i,attributeMicroData), i);
            attributeMicroData = attributesDictionary.get("ddT_");
            updateDdType((int)microDwellings.getValueAt(i,attributeMicroData), i);
            for (int j = 0; j < hhSize; j++){
                int row = (int) microHouseholds.getValueAt(i,"firstPerson") + j;
                int age = (int) microPersons.getValueAt(row,"age");
                int gender = (int) microPersons.getValueAt(row,"Gender");
                int occupation = (int) microPersons.getValueAt(row,"job");
                updateHhAgeGender(age, gender, i);
                updateHhWorkerGender(gender,occupation, i);
            }
            frequencyMatrix.setValueAt(i,"population",hhSize);
        }
        SiloUtil.writeTableDataSet(frequencyMatrix,"input/testing/output/frequencyMatrix.csv");
        logger.info("   Finished creating the frequency matrix");
    }

    private void updateDdType(int ddType, int i) {
        //Method to update the dwelling use
        String ddTypeString = ddTypeDictionary.getIndexedStringValueAt(ddType,"controlTotalLabel");
        frequencyMatrix.setValueAt(i,"ddT_" + ddTypeString , 1);
    }


    private void updateHhWorkerGender(int gender, int occupation, int i) {
        //Method to update the number of workers by gender and job type on the frequency matrix
        String jobTypeString = ppJobDictionary.getIndexedStringValueAt(occupation,"controlTotalLabel");
        if (gender == 1){
            int value = 1 + (int) frequencyMatrix.getValueAt(i, "M_" + jobTypeString);
            frequencyMatrix.setValueAt(i,"M_"+jobTypeString, value);
        } else {
            int value = 1 + (int) frequencyMatrix.getValueAt(i, "F_" + jobTypeString);
            frequencyMatrix.setValueAt(i,"F_"+jobTypeString, value);
        }
    }


    private void updateHhAgeGender(int age, int gender, int i) {
        //method to update the number of persons by gender and age on the frequency matrix
        int row = 0;
        while (age > ageBracketsPerson[row]) {
            row++;
        }
        if (gender == 1){
            int value = 1 + (int) frequencyMatrix.getValueAt(i,"M_" + ageBracketsPerson[row]);
            frequencyMatrix.setValueAt(i,"M_" + ageBracketsPerson[row],value);
        } else {
            int value = 1 + (int) frequencyMatrix.getValueAt(i,"F_" + ageBracketsPerson[row]);
            frequencyMatrix.setValueAt(i,"F_" + ageBracketsPerson[row],value);
        }
    }

   private void updateDdUse(int ddUse, int i) {
        //Method to update the dwelling use on the frequency matrix
       String ddUseString = ddUseDictionary.getIndexedStringValueAt(ddUse,"controlTotalLabel");
       frequencyMatrix.setValueAt(i,"H_" + ddUseString , 1);
    }

    private void updateHhSize(int hhSize, int i) {
        //Method to update the frequency matrix depending on hhSize
        if (hhSize > householdSizes[householdSizes.length - 1]){
            hhSize = householdSizes[householdSizes.length - 1];
        }
        frequencyMatrix.setValueAt(i,"nHH_"+ hhSize, 1);
    }


    private void initializeAttributesMunicipality() {
        //Method to create the list of attributesControlTotal given the generic names and the brackets

        frequencyMatrix = new TableDataSet();
        frequencyMatrix.appendColumn(microHouseholds.getColumnAsInt("id"),"id");

        attributesDictionary = new HashMap<>();

        for (int i = 0; i < attributesControlTotal.length; i++){
            int finish = 0;
            int row = 1;
            String attribute = "";
            while (finish == 0 & row <= variables.getRowCount()){
                String attributeControl = variables.getStringValueAt(row,"VariableNameControlTotal");
                if (attributeControl.equals(attributesControlTotal[i])){
                    finish = 1;
                    attribute = variables.getStringValueAt(row,"VariableNameMicroData");
                } else {
                    row++;
                }
            }
            attributesDictionary.put(attributesControlTotal[i],attribute);
        }
        checkContainsAndAdd("H_",usageBracketsDwelling, attributesDictionary);
        checkContainsAndAdd("ddT_",typeBracketsDwelling, attributesDictionary);
        checkContainsAndAdd("M_",ageBracketsPerson, attributesDictionary);
        checkContainsAndAdd("F_",ageBracketsPerson, attributesDictionary);
        checkContainsAndAdd("M_",occupationBrackets, attributesDictionary);
        checkContainsAndAdd("F_",occupationBrackets, attributesDictionary);
        checkContainsAndAdd("nHH_",householdSizes, attributesDictionary);
        addIntegerColumnToTableDataSet(frequencyMatrix,"population");
        addIntegerColumnToTableDataSet(frequencyMatrix,"hhTotal");
    }

    private void checkContainsAndAdd(String key, String[] brackets, Map<String, String> map) {
        if (map.containsKey(key)){
            for (int i = 0; i < brackets.length; i++){
                String label = key + brackets[i];
                addIntegerColumnToTableDataSet(frequencyMatrix,label);
            }
        }
    }


    private void checkContainsAndAdd(String key, int[] brackets, Map<String, String> map) {
        if (map.containsKey(key)){
            for (int i = 0; i < brackets.length; i++){
                String label = key + brackets[i];
                addIntegerColumnToTableDataSet(frequencyMatrix,label);
            }
        }
    }


    private void initializeMicroData(int hhCountTotal, int personCountTotal){
        //method to initialize the TableDataSets from households, persons and dwellings with the labels from the variables to be read

        microHouseholds = new TableDataSet();
        microPersons = new TableDataSet();
        microDwellings = new TableDataSet();
        addIntegerColumnToTableDataSet(microHouseholds,"id", hhCountTotal);
        addIntegerColumnToTableDataSet(microHouseholds,"H_Code");
        addIntegerColumnToTableDataSet(microHouseholds,"firstPerson");
        addIntegerColumnToTableDataSet(microPersons,"id", personCountTotal);
        addIntegerColumnToTableDataSet(microPersons,"H_Code");
        addIntegerColumnToTableDataSet(microPersons,"idHH");
        addIntegerColumnToTableDataSet(microDwellings,"id",hhCountTotal);

        for (int i = 0; i < attributesMicroData.get("Person").length; i++){
            addIntegerColumnToTableDataSet(microPersons,attributesMicroData.get("Person")[i]);
        }
        for (int i = 0; i < attributesMicroData.get("Household").length; i++){
            addIntegerColumnToTableDataSet(microHouseholds,attributesMicroData.get("Household")[i]);
        }
        for (int i = 0; i < attributesMicroData.get("Dwelling").length; i++){
            addIntegerColumnToTableDataSet(microDwellings,attributesMicroData.get("Dwelling")[i]);
        }

        microHouseholds.buildIndex(microHouseholds.getColumnPosition("id"));
        microPersons.buildIndex(microPersons.getColumnPosition("id"));
        microDwellings.buildIndex(microDwellings.getColumnPosition("id"));
    }


    private TableDataSet addIntegerColumnToTableDataSet(TableDataSet table, String label){
        int[] dummy3 = SiloUtil.createArrayWithValue(table.getRowCount(),0);
        table.appendColumn(dummy3,label);
        return table;
    }


    private TableDataSet addIntegerColumnToTableDataSet(TableDataSet table, String label, int length){
        int[] dummy3 = SiloUtil.createArrayWithValue(length,0);
        table.appendColumn(dummy3,label);
        return table;
    }



}
