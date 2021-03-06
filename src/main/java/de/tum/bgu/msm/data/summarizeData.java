package de.tum.bgu.msm.data;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.autoOwnership.CreateCarOwnershipModel;
import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.SiloUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Methods to summarize model results
 * Author: Rolf Moeckel, PB Albuquerque
 * Created on 23 February 2012 in Albuquerque
 **/


public class summarizeData {
    static Logger logger = Logger.getLogger(summarizeData.class);

    protected static final String PROPERTIES_RESULT_FILE_NAME             = "result.file.name";
    protected static final String PROPERTIES_SPATIAL_RESULT_FILE_NAME     = "spatial.result.file.name";
    protected static final String PROPERTIES_SCALING_YEARS_CONTROL_TOTALS = "scaling.years.control.totals";
    protected static final String PROPERTIES_SCALED_MICRO_DATA_HH         = "scaled.micro.data.hh";
    protected static final String PROPERTIES_SCALED_MICRO_DATA_PP         = "scaled.micro.data.pp";
    protected static final String PROPERTIES_HOUSING_SUMMARY              = "housing.environment.impact.file.name";
    protected static final String PROPERTIES_BEM_YEARS                    = "bem.model.years";
    protected static final String PROPERTIES_FILENAME_HH_MICRODATA        = "household.file.ascii";
    protected static final String PROPERTIES_FILENAME_PP_MICRODATA        = "person.file.ascii";
    protected static final String PROPERTIES_FILENAME_DD_MICRODATA        = "dwelling.file.ascii";
    protected static final String PROPERTIES_FILENAME_JJ_MICRODATA        = "job.file.ascii";
    protected static final String PROPERTIES_WRITE_BIN_POP_FILES          = "write.binary.pop.files";
    protected static final String PROPERTIES_WRITE_BIN_DD_FILE            = "write.binary.dd.file";
    protected static final String PROPERTIES_WRITE_BIN_JJ_FILE            = "write.binary.jj.file";
    protected static final String PROPERTIES_PRESTO_REGION_DEFINITION     = "presto.regions";
    protected static final String PROPERTIES_PRESTO_SUMMARY_FILE          = "presto.summary.file";
    protected static final String PROPERTIES_USE_CAPACITY   = "use.growth.capacity.data";


    private static PrintWriter resultWriter;
    private static PrintWriter spatialResultWriter;

    private static PrintWriter resultWriterFinal;
    private static PrintWriter spatialResultWriterFinal;

    public static Boolean resultWriterReplicate = false;

    private static TableDataSet scalingControlTotals;
    private static int[] prestoRegionByTaz;

    //public geoDataI geoData;


    public static void openResultFile(ResourceBundle rb) {
        // open summary file

        String directory = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName;
        SiloUtil.createDirectoryIfNotExistingYet(directory);
        String resultFileName = rb.getString(PROPERTIES_RESULT_FILE_NAME);
        resultWriter = SiloUtil.openFileForSequentialWriting(directory + "/" + resultFileName +
                SiloUtil.gregorianIterator + ".csv", SiloUtil.getStartYear() != SiloUtil.getBaseYear());
        resultWriterFinal = SiloUtil.openFileForSequentialWriting(directory + "/" + resultFileName + "_" + SiloUtil.getEndYear() + ".csv", false);
    }


    public static void readScalingYearControlTotals (ResourceBundle rb) {
        // read file with control totals to scale synthetic population to exogenous assumptions for selected output years

        String fileName = SiloUtil.baseDirectory + ResourceUtil.getProperty(rb, PROPERTIES_SCALING_YEARS_CONTROL_TOTALS);
        scalingControlTotals = SiloUtil.readCSVfile(fileName);
        scalingControlTotals.buildIndex(scalingControlTotals.getColumnPosition("Zone"));
    }


    public static void resultFile(String action) {
        // handle summary file
        resultFile(action, true);
    }

    public static void resultFile(String action, Boolean writeFinal) {
        // handle summary file
        switch (action) {
            case "close":
                resultWriter.close();
                resultWriterFinal.close();
                break;
            default:
                resultWriter.println(action);
                if(resultWriterReplicate && writeFinal)resultWriterFinal.println(action);
                break;
        }
    }

    public static void resultFileSpatial(ResourceBundle rb, String action) {
        resultFileSpatial(rb,action,true);
    }
    public static void resultFileSpatial(ResourceBundle rb, String action, Boolean writeFinal) {
        // handle summary file
        switch (action) {
            case "open":
                String directory = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName;
                SiloUtil.createDirectoryIfNotExistingYet(directory);
                String resultFileName = rb.getString(PROPERTIES_SPATIAL_RESULT_FILE_NAME);
                spatialResultWriter = SiloUtil.openFileForSequentialWriting(directory + "/" + resultFileName +
                        SiloUtil.gregorianIterator + ".csv", SiloUtil.getStartYear() != SiloUtil.getBaseYear());
                spatialResultWriterFinal = SiloUtil.openFileForSequentialWriting(directory + "/" + resultFileName +"_"+ SiloUtil.getEndYear() + ".csv", false);
                break;
            case "close":
                spatialResultWriter.close();
                spatialResultWriterFinal.close();
                break;
            default:
                spatialResultWriter.println(action);
                if(resultWriterReplicate && writeFinal )spatialResultWriterFinal.println(action);
                break;
        }
    }

    public static void summarizeSpatially (int year, SiloModelContainer modelContainer, SiloDataContainer dataContainer) {
        // write out results by zone

        String hd = "Year" + year + ",autoAccessibility,transitAccessibility,population,households,hhInc_<" + SiloUtil.incBrackets[0];
        for (int inc = 0; inc < SiloUtil.incBrackets.length; inc++) hd = hd.concat(",hhInc_>" + SiloUtil.incBrackets[inc]);
        resultFileSpatial(null, hd + ",dd_SFD,dd_SFA,dd_MF234,dd_MF5plus,dd_MH,availLand,avePrice,jobs,shWhite,shBlack,shHispanic,shOther");

        int[] zones = dataContainer.getGeoData().getZones();
        int[][] dds = new int[DwellingType.values().length + 1][dataContainer.getGeoData().getHighestZonalId() + 1];
        int[] prices = new int[dataContainer.getGeoData().getHighestZonalId() + 1];
        int[] jobs = new int[dataContainer.getGeoData().getHighestZonalId() + 1];
        int[] hhs = new int[dataContainer.getGeoData().getHighestZonalId() + 1];
        int[][] hhInc = new int[SiloUtil.incBrackets.length + 1][dataContainer.getGeoData().getHighestZonalId() + 1];
        int[] pop = getPopulationByZone(dataContainer.getGeoData());
        for (Household hh: Household.getHouseholdArray()) {
            int zone = Dwelling.getDwellingFromId(hh.getDwellingId()).getZone();
            int incGroup = HouseholdDataManager.getIncomeCategoryForIncome(hh.getHhIncome());
            hhInc[incGroup - 1][zone]++;
            hhs[zone] ++;
        }
        for (Dwelling dd: Dwelling.getDwellingArray()) {
            dds[dd.getType().ordinal()][dd.getZone()]++;
            prices[dd.getZone()] += dd.getPrice();
        }
        for (Job jj: Job.getJobArray()) {
            jobs[jj.getZone()]++;
        }


        for (int taz: zones) {
            float avePrice = -1;
            int ddThisZone = 0;
            for (DwellingType dt: DwellingType.values()) ddThisZone += dds[dt.ordinal()][taz];
            if (ddThisZone > 0) avePrice = prices[taz] / ddThisZone;
            double autoAcc = modelContainer.getAcc().getAutoAccessibility(taz);
            double transitAcc = modelContainer.getAcc().getTransitAccessibility(taz);
            double availLand = dataContainer.getRealEstateData().getAvailableLandForConstruction(taz);
//            Formatter f = new Formatter();
//            f.format("%d,%f,%f,%d,%d,%d,%f,%f,%d", taz, autoAcc, transitAcc, pop[taz], hhs[taz], dds[taz], availLand, avePrice, jobs[taz]);
            String txt = taz + "," + autoAcc + "," + transitAcc + "," + pop[taz] + "," + hhs[taz];
            for (int inc = 0; inc <= SiloUtil.incBrackets.length; inc++) txt = txt.concat("," + hhInc[inc][taz]);
            for (DwellingType dt: DwellingType.values()) txt = txt.concat("," + dds[dt.ordinal()][taz]);
            txt = txt.concat("," + availLand + "," + avePrice + "," + jobs[taz] + "," +
                    // todo: make the summary application specific, Munich does not work with these race categories
                    "0,0,0,0");
//                    modelContainer.getMove().getZonalRacialShare(taz, Race.white) + "," +
//                    modelContainer.getMove().getZonalRacialShare(taz, Race.black) + "," +
//                    modelContainer.getMove().getZonalRacialShare(taz, Race.hispanic) + "," +
//                    modelContainer.getMove().getZonalRacialShare(taz, Race.other));
//            String txt = f.toString();
            resultFileSpatial(null, txt);
        }
    }


    public static int[] getPopulationByZone (geoDataI geoData) {
        // summarize population by zone

        int[] pp = new int[geoData.getHighestZonalId() + 1];
        for (Household hh: Household.getHouseholdArray()) {
            int zone = Dwelling.getDwellingFromId(hh.getDwellingId()).getZone();
            pp[zone] += hh.getHhSize();
        }
        return pp;
    }


    public int[] getHouseholdsByZone (geoDataI geoData) {
        // summarize households by zone

        int[] householdsByZone = new int[geoData.getZones().length];
        for (Household hh: Household.getHouseholdArray()) {
            int zone = Dwelling.getDwellingFromId(hh.getDwellingId()).getZone();
            householdsByZone[geoData.getZoneIndex(zone)]++;
        }
        return householdsByZone;
    }


    public static int[] getRetailEmploymentByZone(geoDataI geoData) {
        // summarize retail employment by zone

        int[] retailEmplByZone = new int[geoData.getZones().length];
        for (Job jj: Job.getJobArray()) {
            if (jj.getType().equals("RET")) retailEmplByZone[geoData.getZoneIndex(jj.getZone())]++;
        }
        return retailEmplByZone;
    }


    public static int[] getOfficeEmploymentByZone(geoDataI geoData) {
        // summarize office employment by zone

        int[] officeEmplByZone = new int[geoData.getZones().length];
        for (Job jj: Job.getJobArray()) {
            if (jj.getType().equals("OFF")) officeEmplByZone[geoData.getZoneIndex(jj.getZone())]++;
        }
        return officeEmplByZone;
    }


    public static int[] getOtherEmploymentByZone(geoDataI geoData) {
        // summarize other employment by zone

        int[] otherEmplByZone = new int[geoData.getZones().length];
        for (Job jj: Job.getJobArray()) {
            if (jj.getType().equals("OTH")) otherEmplByZone[geoData.getZoneIndex(jj.getZone())]++;
        }
        return otherEmplByZone;
    }


    public static int[] getTotalEmploymentByZone(geoDataI geoData) {
        // summarize retail employment by zone

        int[] totalEmplByZone = new int[geoData.getZones().length];
        for (Job jj: Job.getJobArray()) {
            totalEmplByZone[geoData.getZoneIndex(jj.getZone())]++;
        }
        return totalEmplByZone;
    }


    public static void scaleMicroDataToExogenousForecast (ResourceBundle rb, int year, SiloDataContainer dataContainer) {
        //TODO Will fail for new zones with 0 households and a projected growth. Could be an issue when modeling for Zones with transient existence.
        // scale synthetic population to exogenous forecast (for output only, scaled synthetic population is not used internally)

        if (!scalingControlTotals.containsColumn(("HH" + year))) {
            logger.warn("Could not find scaling targets to scale micro data to year " + year + ". No scaling completed.");
            return;
        }
        logger.info("Scaling synthetic population to exogenous forecast for year " + year + " (for output only, " +
                "scaled population is not used internally).");

        int artificialHhId = HouseholdDataManager.getHighestHouseholdIdInUse() + 1;
        int artificialPpId = HouseholdDataManager.getHighestPersonIdInUse() + 1;

        // calculate how many households need to be created or deleted in every zone
        int[] changeOfHh = new int[(dataContainer.getGeoData().getHighestZonalId() + 1)];
        HashMap<Integer, int[]> hhByZone = dataContainer.getHouseholdData().getHouseholdsByZone();
        for (int zone: dataContainer.getGeoData().getZones()) {
            int hhs = 0;
            if (hhByZone.containsKey(zone)) hhs = hhByZone.get(zone).length;
            changeOfHh[zone] =
                    (int) scalingControlTotals.getIndexedValueAt(zone, ("HH" + year)) - hhs;
        }

        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(rb.getString(PROPERTIES_SCALED_MICRO_DATA_HH) + year + ".csv", false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(rb.getString(PROPERTIES_SCALED_MICRO_DATA_PP) + year + ".csv", false);
        pwp.println("id,hhID,age,gender,race,occupation,driversLicense,workplace,income");
        for (int zone: dataContainer.getGeoData().getZones()) {
            if (hhByZone.containsKey(zone)) {
                int[] hhInThisZone = hhByZone.get(zone);
                int[] selectedHH = new int[hhInThisZone.length];
                if (changeOfHh[zone] > 0) {          // select households to duplicate (draw with replacement)
                    for (int i = 0; i < changeOfHh[zone]; i++) {
                        int selected = SiloUtil.select(hhInThisZone.length) - 1;
                        selectedHH[selected]++;
                    }
                } else if (changeOfHh[zone] < 0) {   // select households to delete (draw without replacement)
                    float[] prob = new float[hhInThisZone.length];
                    SiloUtil.setArrayToValue(prob, 1);
                    for (int i = 0; i < Math.abs(changeOfHh[zone]); i++) {
                        int selected = SiloUtil.select(prob);
                        selectedHH[selected] = 1;
                        prob[selected] = 0;
                    }
                }

                // write out households and duplicate (if changeOfHh > 0) or delete (if changeOfHh < 0) selected households
                for (int i = 0; i < hhInThisZone.length; i++) {
                    Household hh = Household.getHouseholdFromId(hhInThisZone[i]);
                    if (changeOfHh[zone] > 0) {
                        // write out original household
                        pwh.print(hh.getId());
                        pwh.print(",");
                        pwh.print(hh.getDwellingId());
                        pwh.print(",");
                        pwh.print(hh.getHomeZone());
                        pwh.print(",");
                        pwh.print(hh.getHhSize());
                        pwh.print(",");
                        pwh.println(hh.getAutos());
                        for (Person pp: hh.getPersons()) {
                            pwp.print(pp.getId());
                            pwp.print(",");
                            pwp.print(pp.getHhId());
                            pwp.print(",");
                            pwp.print(pp.getAge());
                            pwp.print(",");
                            pwp.print(pp.getGender());
                            pwp.print(",");
                            pwp.print(pp.getRace());
                            pwp.print(",");
                            pwp.print(pp.getOccupation());
                            pwp.print(",0,");
                            pwp.print(pp.getWorkplace());
                            pwp.print(",");
                            pwp.println(pp.getIncome());
                        }
                        // duplicate household if selected
                        if (selectedHH[i] > 0) {    // household to be repeated for this output file
                            for (int repeat = 0; repeat < selectedHH[i]; repeat++) {
                                pwh.print(artificialHhId);
                                pwh.print(",");
                                pwh.print(hh.getDwellingId());
                                pwh.print(",");
                                pwh.print(hh.getHomeZone());
                                pwh.print(",");
                                pwh.print(hh.getHhSize());
                                pwh.print(",");
                                pwh.println(hh.getAutos());
                                for (Person pp: hh.getPersons()) {
                                    pwp.print(artificialPpId);
                                    pwp.print(",");
                                    pwp.print(artificialHhId);
                                    pwp.print(",");
                                    pwp.print(pp.getAge());
                                    pwp.print(",");
                                    pwp.print(pp.getGender());
                                    pwp.print(",");
                                    pwp.print(pp.getRace());
                                    pwp.print(",");
                                    pwp.print(pp.getOccupation());
                                    pwp.print(",0,");
                                    pwp.print(pp.getWorkplace());
                                    pwp.print(",");
                                    pwp.println(pp.getIncome());
                                    artificialPpId++;
                                }
                                artificialHhId++;
                            }
                        }
                    } else if (changeOfHh[zone] < 0) {
                        if (selectedHH[i] == 0) {    // household to be kept (selectedHH[i] == 1 for households to be deleted)
                            pwh.print(hh.getId());
                            pwh.print(",");
                            pwh.print(hh.getDwellingId());
                            pwh.print(",");
                            pwh.print(hh.getHomeZone());
                            pwh.print(",");
                            pwh.print(hh.getHhSize());
                            pwh.print(",");
                            pwh.println(hh.getAutos());
                            for (Person pp: hh.getPersons()) {
                                pwp.print(pp.getId());
                                pwp.print(",");
                                pwp.print(pp.getHhId());
                                pwp.print(",");
                                pwp.print(pp.getAge());
                                pwp.print(",");
                                pwp.print(pp.getGender());
                                pwp.print(",");
                                pwp.print(pp.getRace());
                                pwp.print(",");
                                pwp.print(pp.getOccupation());
                                pwp.print(",0,");
                                pwp.print(pp.getWorkplace());
                                pwp.print(",");
                                pwp.println(pp.getIncome());
                            }
                        }
                    }
                }
            } else {
                if (scalingControlTotals.getIndexedValueAt(zone, ("HH" + year)) > 0) logger.warn("SILO has no households in zone " +
                        zone + " that could be duplicated to match control total of " +
                        scalingControlTotals.getIndexedValueAt(zone, ("HH" + year)) + ".");
            }
        }
        pwh.close();
        pwp.close();
    }


    public static void summarizeHousing (ResourceBundle rb, int year) {
        // summarize housing data for housing environmental impact calculations

        if (!SiloUtil.containsElement(ResourceUtil.getIntegerArray(rb, PROPERTIES_BEM_YEARS), year)) return;
        String directory = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/bem/";
        SiloUtil.createDirectoryIfNotExistingYet(directory);

        String fileName = (directory + rb.getString(PROPERTIES_HOUSING_SUMMARY) + "_" + year + "_" +
                SiloUtil.gregorianIterator + ".csv");

        PrintWriter pw = SiloUtil.openFileForSequentialWriting(fileName, false);
        pw.println("id,zone,type,size,yearBuilt,occupied");
        for (Dwelling dd: Dwelling.getDwellingArray()){
            pw.print(dd.getId());
            pw.print(",");
            pw.print(dd.getZone());
            pw.print(",");
            pw.print(dd.getType());
            pw.print(",");
            pw.print(dd.getBedrooms());
            pw.print(",");
            pw.print(dd.getYearBuilt());
            pw.print(",");
            pw.println((dd.getResidentId() == -1));
        }
        pw.close();
    }


    public static void writeOutSyntheticPopulation (ResourceBundle rb, int year) {
        // write out files with synthetic population

        logger.info("  Writing household file");
        String filehh = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_HH_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(filehh, false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        Household[] hhs = Household.getHouseholdArray();
        for (Household hh : hhs) {
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                hh.logAttributes(SiloUtil.trackWriter);
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getDwellingId());
            pwh.print(",");
            pwh.print(hh.getHomeZone());
            pwh.print(",");
            pwh.print(hh.getHhSize());
            pwh.print(",");
            pwh.println(hh.getAutos());
        }
        pwh.close();

        logger.info("  Writing person file");
        String filepp = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_PP_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(filepp, false);
        pwp.println("id,hhID,age,gender,relationShip,race,occupation,driversLicense,workplace,income");
        Person[] pps = Person.getPersonArray();
        for (Person pp : pps) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHhId());
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender());
            pwp.print(",\"");
            pwp.print(pp.getRole());
            pwp.print("\",\"");
            pwp.print(pp.getRace());
            pwp.print("\",");
            pwp.print(pp.getOccupation());
            pwp.print(",0,");
            pwp.print(pp.getWorkplace());
            pwp.print(",");
            pwp.println(pp.getIncome());
            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                pp.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwp.close();

        logger.info("  Writing dwelling file");
        String filedd = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_DD_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(filedd, false);
        pwd.println("id,zone,type,hhID,bedrooms,quality,monthlyCost,restriction,yearBuilt");
        Dwelling[] dds = Dwelling.getDwellingArray();
        for (Dwelling dd : dds) {
            pwd.print(dd.getId());
            pwd.print(",");
            pwd.print(dd.getZone());
            pwd.print(",\"");
            pwd.print(dd.getType());
            pwd.print("\",");
            pwd.print(dd.getResidentId());
            pwd.print(",");
            pwd.print(dd.getBedrooms());
            pwd.print(",");
            pwd.print(dd.getQuality());
            pwd.print(",");
            pwd.print(dd.getPrice());
            pwd.print(",");
            pwd.print(dd.getRestriction());
            pwd.print(",");
            pwd.println(dd.getYearBuilt());
            if (dd.getId() == SiloUtil.trackDd) {
                SiloUtil.trackingFile("Writing dd " + dd.getId() + " to micro data file.");
                dd.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwd.close();

/*        logger.info ("  Reading dwelling file that was written (for debugging only");
        String recString = "";
        int recCount = 0;
        try {
            File file = new File(filedd);
            if (file.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(file));
                while ((recString = in.readLine()) != null) {
                    recCount++;
                    System.out.println(recCount+" <"+recString+">");
                }
            } else {
                System.out.println("Did not find file " + filedd);
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop dwelling file: " + filedd);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }*/



        logger.info("  Writing job file");
        String filejj = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_JJ_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwj = SiloUtil.openFileForSequentialWriting(filejj, false);
        pwj.println("id,zone,personId,type");
        Job[] jjs = Job.getJobArray();
        for (Job jj : jjs) {
            pwj.print(jj.getId());
            pwj.print(",");
            pwj.print(jj.getZone());
            pwj.print(",");
            pwj.print(jj.getWorkerId());
            pwj.print(",\"");
            pwj.print(jj.getType());
            pwj.println("\"");
            if (jj.getId() == SiloUtil.trackJj) {
                SiloUtil.trackingFile("Writing jj " + jj.getId() + " to micro data file.");
                jj.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwj.close();

        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_POP_FILES))
            HouseholdDataManager.writeBinaryPopulationDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_DD_FILE))
            RealEstateDataManager.writeBinaryDwellingDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_JJ_FILE))
            JobDataManager.writeBinaryJobDataObjects(rb);
    }


    public static void writeOutSyntheticPopulationDE (ResourceBundle rb, int year) {
        // write out files with synthetic population

        logger.info("  Writing household file");
        String filehh = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_HH_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(filehh, false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        Household[] hhs = Household.getHouseholdArray();
        for (Household hh : hhs) {
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                hh.logAttributes(SiloUtil.trackWriter);
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getDwellingId());
            pwh.print(",");
            pwh.print(hh.getHomeZone());
            pwh.print(",");
            pwh.print(hh.getHhSize());
            pwh.print(",");
            pwh.println(hh.getAutos());

        }
        pwh.close();

        logger.info("  Writing person file");
        String filepp = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_PP_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(filepp, false);
        pwp.println("id,hhid,age,gender,relationShip,race,occupation,workplace,income,nationality,education,homeZone,workZone,driversLicense,schoolDE");
        Person[] pps = Person.getPersonArray();
        for (Person pp : pps) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHhId());
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender());
            pwp.print(",\"");
            pwp.print(pp.getRole());
            pwp.print("\",\"");
            pwp.print(pp.getRace());
            pwp.print("\",");
            pwp.print(pp.getOccupation());
            pwp.print(",");
            pwp.print(pp.getWorkplace());
            pwp.print(",");
            pwp.print(pp.getIncome());
            pwp.print(",");
            pwp.print(pp.getNationality());
            pwp.print(",");
            pwp.print(pp.getEducationLevel());
            pwp.print(",");
            pwp.print(pp.getZone());
            pwp.print(",");
            pwp.print(pp.getJobTAZ());
            pwp.print(",");
            pwp.print(pp.getDriverLicense());
            pwp.print(",");
            pwp.println(pp.getSchoolType());
            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                pp.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwp.close();

        logger.info("  Writing dwelling file");
        String filedd = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_DD_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(filedd, false);
        pwd.println("id,zone,type,hhID,bedrooms,quality,monthlyCost,restriction,yearBuilt,floor,building,year,usage");
        Dwelling[] dds = Dwelling.getDwellingArray();
        for (Dwelling dd : dds) {
            pwd.print(dd.getId());
            pwd.print(",");
            pwd.print(dd.getZone());
            pwd.print(",\"");
            pwd.print(dd.getType());
            pwd.print("\",");
            pwd.print(dd.getResidentId());
            pwd.print(",");
            pwd.print(dd.getBedrooms());
            pwd.print(",");
            pwd.print(dd.getQuality());
            pwd.print(",");
            pwd.print(dd.getPrice());
            pwd.print(",");
            pwd.print(dd.getRestriction());
            pwd.print(",");
            pwd.print(dd.getYearBuilt());
            pwd.print(",");
            pwd.print(dd.getFloorSpace());
            pwd.print(",");
            pwd.print(dd.getBuildingSize());
            pwd.print(",");
            pwd.print(dd.getYearConstructionDE());
            pwd.print(",");
            pwd.println(dd.getUsage());
            if (dd.getId() == SiloUtil.trackDd) {
                SiloUtil.trackingFile("Writing dd " + dd.getId() + " to micro data file.");
                dd.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwd.close();

        logger.info("  Writing job file");
        String filejj = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_JJ_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwj = SiloUtil.openFileForSequentialWriting(filejj, false);
        pwj.println("id,zone,personId,type");
        Job[] jjs = Job.getJobArray();
        for (Job jj : jjs) {
            pwj.print(jj.getId());
            pwj.print(",");
            pwj.print(jj.getZone());
            pwj.print(",");
            pwj.print(jj.getWorkerId());
            pwj.print(",");
            pwj.println(jj.getType());
            if (jj.getId() == SiloUtil.trackJj) {
                SiloUtil.trackingFile("Writing jj " + jj.getId() + " to micro data file.");
                jj.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwj.close();

/*
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_POP_FILES))
            HouseholdDataManager.writeBinaryPopulationDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_DD_FILE))
            RealEstateDataManager.writeBinaryDwellingDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_JJ_FILE))
            JobDataManager.writeBinaryJobDataObjects(rb);
*/

    }



    public static void writeOutSyntheticPopulationDE (ResourceBundle rb, int year, String file) {
        // write out files with synthetic population

        logger.info("  Writing household file");
        String filehh = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_HH_MICRODATA) + file +
                year + ".csv";
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(filehh, false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        Household[] hhs = Household.getHouseholdArray();
        for (Household hh : hhs) {
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                hh.logAttributes(SiloUtil.trackWriter);
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getDwellingId());
            pwh.print(",");
            pwh.print(hh.getHomeZone());
            pwh.print(",");
            pwh.print(hh.getHhSize());
            pwh.print(",");
            pwh.println(hh.getAutos());

        }
        pwh.close();

        logger.info("  Writing person file");
        String filepp = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_PP_MICRODATA) + file +
                year + ".csv";
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(filepp, false);
        pwp.println("id,hhid,age,gender,relationShip,race,occupation,workplace,income,nationality,education,homeZone,workZone,driversLicense,schoolDE,schoolplace,autos,trips");
        Person[] pps = Person.getPersonArray();
        for (Person pp : pps) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHhId());
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender());
            pwp.print(",\"");
            pwp.print(pp.getRole());
            pwp.print("\",\"");
            pwp.print(pp.getRace());
            pwp.print("\",");
            pwp.print(pp.getOccupation());
            pwp.print(",");
            pwp.print(pp.getWorkplace());
            pwp.print(",");
            pwp.print(pp.getIncome());
            pwp.print(",");
            pwp.print(pp.getNationality());
            pwp.print(",");
            pwp.print(pp.getEducationLevel());
            pwp.print(",");
            pwp.print(pp.getZone());
            pwp.print(",");
            pwp.print(pp.getJobTAZ());
            pwp.print(",");
            pwp.print(pp.getDriverLicense());
            pwp.print(",");
            pwp.print(pp.getSchoolType());
            pwp.print(",");
            pwp.print(pp.getSchoolPlace());
            pwp.print(",");
            pwp.print(Household.getHouseholdFromId(pp.getHhId()).getAutos());
            pwp.print(",");
            pwp.println(pp.getTelework());
            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                pp.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwp.close();

        logger.info("  Writing dwelling file");
        String filedd = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_DD_MICRODATA) + file +
                year + ".csv";
        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(filedd, false);
        pwd.println("id,zone,type,hhID,bedrooms,quality,monthlyCost,restriction,yearBuilt,floor,building,year,usage");
        Dwelling[] dds = Dwelling.getDwellingArray();
        for (Dwelling dd : dds) {
            pwd.print(dd.getId());
            pwd.print(",");
            pwd.print(dd.getZone());
            pwd.print(",\"");
            pwd.print(dd.getType());
            pwd.print("\",");
            pwd.print(dd.getResidentId());
            pwd.print(",");
            pwd.print(dd.getBedrooms());
            pwd.print(",");
            pwd.print(dd.getQuality());
            pwd.print(",");
            pwd.print(dd.getPrice());
            pwd.print(",");
            pwd.print(dd.getRestriction());
            pwd.print(",");
            pwd.print(dd.getYearBuilt());
            pwd.print(",");
            pwd.print(dd.getFloorSpace());
            pwd.print(",");
            pwd.print(dd.getBuildingSize());
            pwd.print(",");
            pwd.print(dd.getYearConstructionDE());
            pwd.print(",");
            pwd.println(dd.getUsage());
            if (dd.getId() == SiloUtil.trackDd) {
                SiloUtil.trackingFile("Writing dd " + dd.getId() + " to micro data file.");
                dd.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwd.close();

        logger.info("  Writing job file");
        String filejj = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_JJ_MICRODATA) + file +
                year + ".csv";
        PrintWriter pwj = SiloUtil.openFileForSequentialWriting(filejj, false);
        pwj.println("id,zone,personId,type");
        Job[] jjs = Job.getJobArray();
        for (Job jj : jjs) {
            pwj.print(jj.getId());
            pwj.print(",");
            pwj.print(jj.getZone());
            pwj.print(",");
            pwj.print(jj.getWorkerId());
            pwj.print(",");
            pwj.println(jj.getType());
            if (jj.getId() == SiloUtil.trackJj) {
                SiloUtil.trackingFile("Writing jj " + jj.getId() + " to micro data file.");
                jj.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwj.close();

/*
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_POP_FILES))
            HouseholdDataManager.writeBinaryPopulationDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_DD_FILE))
            RealEstateDataManager.writeBinaryDwellingDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_JJ_FILE))
            JobDataManager.writeBinaryJobDataObjects(rb);
*/

    }

    public static void writeOutSyntheticPopulationDEShort (ResourceBundle rb, int year, int step) {
        // write out files with synthetic population

        String fileEnding = "_" + step + "k_" + year + ".csv";

        logger.info("  Writing household file");
        String filehh = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_HH_MICRODATA) + fileEnding;
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(filehh, false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        Household[] hhs = Household.getHouseholdArray();
        String filepp = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_PP_MICRODATA) + fileEnding;
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(filepp, false);
        pwp.println("id,hhid,age,gender,relationShip,race,occupation,workplace,income,nationality,education,homeZone,workZone,license,schoolDE");
        Person[] pps = Person.getPersonArray();

        for (int i = 0; i < hhs.length; i = i + step) {
            Household hh = hhs[i];
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                hh.logAttributes(SiloUtil.trackWriter);
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getDwellingId());
            pwh.print(",");
            pwh.print(hh.getHomeZone());
            pwh.print(",");
            pwh.print(hh.getHhSize());
            pwh.print(",");
            pwh.println(hh.getAutos());
            for (int j = 0; j < hh.getHhSize(); j++){
                Person pp = hh.getPersons()[j];
                pwp.print(pp.getId());
                pwp.print(",");
                pwp.print(pp.getHhId());
                pwp.print(",");
                pwp.print(pp.getAge());
                pwp.print(",");
                pwp.print(pp.getGender());
                pwp.print(",\"");
                pwp.print(pp.getRole());
                pwp.print("\",\"");
                pwp.print(pp.getRace());
                pwp.print("\",");
                pwp.print(pp.getOccupation());
                pwp.print(",");
                pwp.print(pp.getWorkplace());
                pwp.print(",");
                pwp.print(pp.getIncome());
                pwp.print(",");
                pwp.print(pp.getNationality());
                pwp.print(",");
                pwp.print(pp.getEducationLevel());
                pwp.print(",");
                pwp.print(pp.getZone());
                pwp.print(",");
                pwp.print(pp.getJobTAZ());
                pwp.print(",");
                pwp.print(pp.getDriverLicense());
                pwp.print(",");
                pwp.println(pp.getSchoolType());
                if (pp.getId() == SiloUtil.trackPp) {
                    SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                    pp.logAttributes(SiloUtil.trackWriter);
                }
            }
        }
        pwh.close();
        pwp.close();

        logger.info("  Writing dwelling file");
        String filedd = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_DD_MICRODATA) + fileEnding;
        PrintWriter pwd = SiloUtil.openFileForSequentialWriting(filedd, false);
        pwd.println("id,zone,type,hhID,bedrooms,quality,monthlyCost,restriction,yearBuilt,floor,building,year,usage");
        Dwelling[] dds = Dwelling.getDwellingArray();
        for (int i = 0; i < dds.length; i = i + step) {
            Dwelling dd = dds[i];
            pwd.print(dd.getId());
            pwd.print(",");
            pwd.print(dd.getZone());
            pwd.print(",\"");
            pwd.print(dd.getType());
            pwd.print("\",");
            pwd.print(dd.getResidentId());
            pwd.print(",");
            pwd.print(dd.getBedrooms());
            pwd.print(",");
            pwd.print(dd.getQuality());
            pwd.print(",");
            pwd.print(dd.getPrice());
            pwd.print(",");
            pwd.print(dd.getRestriction());
            pwd.print(",");
            pwd.print(dd.getYearBuilt());
            pwd.print(",");
            pwd.print(dd.getFloorSpace());
            pwd.print(",");
            pwd.print(dd.getBuildingSize());
            pwd.print(",");
            pwd.print(dd.getYearConstructionDE());
            pwd.print(",");
            pwd.println(dd.getUsage());
            if (dd.getId() == SiloUtil.trackDd) {
                SiloUtil.trackingFile("Writing dd " + dd.getId() + " to micro data file.");
                dd.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwd.close();

        logger.info("  Writing job file");
        String filejj = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_JJ_MICRODATA) + fileEnding;
        PrintWriter pwj = SiloUtil.openFileForSequentialWriting(filejj, false);
        pwj.println("id,zone,personId,type");
        Job[] jjs = Job.getJobArray();
        for (int i = 0; i < jjs.length; i = i + step) {
            Job jj = jjs[i];
            pwj.print(jj.getId());
            pwj.print(",");
            pwj.print(jj.getZone());
            pwj.print(",");
            pwj.print(jj.getWorkerId());
            pwj.print(",");
            pwj.println(jj.getType());
            if (jj.getId() == SiloUtil.trackJj) {
                SiloUtil.trackingFile("Writing jj " + jj.getId() + " to micro data file.");
                jj.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwj.close();


    }


    public static void summarizeAutoOwnershipByCounty(Accessibility accessibility, JobDataManager jobData) {
        // This calibration function summarized households by auto-ownership and quits

        PrintWriter pwa = SiloUtil.openFileForSequentialWriting("autoOwnershipA.csv", false);
        pwa.println("hhSize,workers,income,transit,density,autos");
        int[][] autos = new int[4][60000];
        for (Household hh: Household.getHouseholdArray()) {
            int autoOwnership = hh.getAutos();
            int zone = hh.getHomeZone();
            int county = geoDataMstm.getCountyOfZone(zone);
            autos[autoOwnership][county]++;
            pwa.println(hh.getHhSize()+","+hh.getNumberOfWorkers()+","+hh.getHhIncome()+","+
                    accessibility.getTransitAccessibility(zone)+","+jobData.getJobDensityInZone(zone)+","+hh.getAutos());
        }
        pwa.close();

        PrintWriter pw = SiloUtil.openFileForSequentialWriting("autoOwnershipB.csv", false);
        pw.println("County,0autos,1auto,2autos,3+autos");
        for (int county = 0; county < 60000; county++) {
            int sm = 0;
            for (int a = 0; a < 4; a++) sm += autos[a][county];
            if (sm > 0) pw.println(county+","+autos[0][county]+","+autos[1][county]+","+autos[2][county]+","+autos[3][county]);
        }
        pw.close();
        logger.info("Summarized auto ownership and quit.");
        System.exit(0);
    }


    public static void preparePrestoSummary (ResourceBundle rb, geoDataI geoData) {
        // open PRESTO summary file

        String prestoZoneFile = SiloUtil.baseDirectory + rb.getString(PROPERTIES_PRESTO_REGION_DEFINITION);
        TableDataSet regionDefinition = SiloUtil.readCSVfile(prestoZoneFile);
        regionDefinition.buildIndex(regionDefinition.getColumnPosition("aggFips"));

        prestoRegionByTaz = SiloUtil.createArrayWithValue((geoData.getHighestZonalId() + 1), -1);
        for (int zone: geoData.getZones()) {
            try {
                prestoRegionByTaz[zone] =
                        (int) regionDefinition.getIndexedValueAt(geoDataMstm.getCountyOfZone(zone), "presto");
            } catch (Exception e) {
                prestoRegionByTaz[zone] = -1;
            }
        }
    }


    public static void summarizePrestoRegion (ResourceBundle rb, int year) {
        // summarize housing costs by income group in SILO region

        String fileName = (SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/" +
                rb.getString(PROPERTIES_PRESTO_SUMMARY_FILE) + SiloUtil.gregorianIterator + ".csv");
        PrintWriter pw = SiloUtil.openFileForSequentialWriting(fileName, year != SiloUtil.getBaseYear());
        pw.println(year + ",Housing costs by income group");
        pw.print("Income");
        for (int i = 0; i < 10; i++) pw.print(",rent_" + ((i + 1) * 250));
        pw.println(",averageRent");
        int[][] rentByIncome = new int[10][10];
        int[] rents = new int[10];
        for (Household hh: Household.getHouseholdArray()) {
            if (prestoRegionByTaz[hh.getHomeZone()] > 0) {
                int hhInc = hh.getHhIncome();
                int rent = Dwelling.getDwellingFromId(hh.getDwellingId()).getPrice();
                int incCat = Math.min((hhInc / 10000), 9);
                int rentCat = Math.min((rent / 250), 9);
                rentByIncome[incCat][rentCat]++;
                rents[incCat] += rent;
            }
        }
        for (int i = 0; i < 10; i++) {
            pw.print(String.valueOf((i + 1) * 10000));
            int countThisIncome = 0;
            for (int r = 0; r < 10; r++) {
                pw.print("," + rentByIncome[i][r]);
                countThisIncome += rentByIncome[i][r];
            }
            pw.println("," + rents[i] / countThisIncome);
        }
    }


    public static void writeOutSyntheticPopulationDe (ResourceBundle rb, int year) {
        // write out files with synthetic population

        logger.info("  Writing household file");
        String filehh = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_HH_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(filehh, false);
        pwh.println("id,dwelling,zone,hhSize,autos");
        Household[] hhs = Household.getHouseholdArray();
        for (Household hh : hhs) {
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                hh.logAttributes(SiloUtil.trackWriter);
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getDwellingId());
            pwh.print(",");
            pwh.print(hh.getHomeZone());
            pwh.print(",");
            pwh.print(hh.getHhSize());
            pwh.print(",");
            pwh.println(hh.getAutos());
        }
        pwh.close();

        logger.info("  Writing person file");
        String filepp = SiloUtil.baseDirectory + rb.getString(PROPERTIES_FILENAME_PP_MICRODATA) + "_" +
                year + ".csv";
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(filepp, false);
        pwp.println("id,hhID,age,gender,relationShip,race,occupation,driversLicense,workplace,income");
        Person[] pps = Person.getPersonArray();
        for (Person pp : pps) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHhId());
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender());
            pwp.print(",\"");
            pwp.print(pp.getRole());
            pwp.print("\",\"");
            pwp.print(pp.getRace());
            pwp.print("\",");
            pwp.print(pp.getOccupation());
            pwp.print(",0,");
            pwp.print(pp.getWorkplace());
            pwp.print(",");
            pwp.println(pp.getIncome());
            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                pp.logAttributes(SiloUtil.trackWriter);
            }
        }
        pwp.close();

        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_POP_FILES))
            HouseholdDataManager.writeBinaryPopulationDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_DD_FILE))
            RealEstateDataManager.writeBinaryDwellingDataObjects(rb);
        if (ResourceUtil.getBooleanProperty(rb, PROPERTIES_WRITE_BIN_JJ_FILE))
            JobDataManager.writeBinaryJobDataObjects(rb);
    }

    public static void summarizeCarOwnershipByMunicipality(TableDataSet zonalData) {
        // This calibration function summarizes household auto-ownership by municipality and quits

        PrintWriter pwa = SiloUtil.openFileForSequentialWriting("microData/interimFiles/carOwnershipA.csv", false);
        pwa.println("license,workers,income,logDistanceToTransit,areaType,autos");
        int[][] autos = new int[4][10000000];
        for (Household hh: Household.getHouseholdArray()) {
            int autoOwnership = hh.getAutos();
            int zone = hh.getHomeZone();
            int municipality = (int) zonalData.getIndexedValueAt(zone, "ID_city");
            int distance = (int) Math.log(zonalData.getIndexedValueAt(zone, "distanceToTransit"));
            int area = (int) zonalData.getIndexedValueAt(zone,"BBSR");
            autos[autoOwnership][municipality]++;
            pwa.println(hh.getHHLicenseHolders()+ "," + hh.getNumberOfWorkers() + "," + hh.getHhIncome() + "," +
                    distance + "," + area + "," + hh.getAutos());
        }
        pwa.close();

        PrintWriter pw = SiloUtil.openFileForSequentialWriting("microData/interimFiles/carOwnershipB.csv", false);
        pw.println("Municipality,0autos,1auto,2autos,3+autos");
        for (int municipality = 0; municipality < 10000000; municipality++) {
            int sm = 0;
            for (int a = 0; a < 4; a++) sm += autos[a][municipality];
            if (sm > 0) pw.println(municipality+","+autos[0][municipality]+","+autos[1][municipality]+","+autos[2][municipality]+","+autos[3][municipality]);
        }
        pw.close();

        logger.info("Summarized auto ownership and quit.");
        //System.exit(0);

    }
}
