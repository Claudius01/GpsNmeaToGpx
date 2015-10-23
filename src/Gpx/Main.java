package Gpx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;

import Misc.Formater;

import com.topografix.gpx.CommandResponseData;
import com.topografix.gpx.GpxType;
import com.topografix.gpx.TrkType;
import com.topografix.gpx.TrksegType;
import com.topografix.gpx.WptType;

// cf. http://www.codeshttp.com/iso88591.htm pour les caracteres encodee dans le xml

public class Main {

	private static Logger logger = Logger.getLogger(Main.class);

	private static String configPath = "etc";
	private static String configFile = "gpxConfiguration.xml";

	// Static configuration (default values)
	// Remarque: No default parameters
	// End of static configuration

	// Constants
	private static final int INDENT_PLUS = 1, INDENT_NONE = 0, INDENT_MOINS = -1;
	private static final int SIZE_BUFFER_READ = 1024;	// Taille du buffer de lecture dans le fichier 'inputFileName'

	// Variables issue de la configuration
	private static Configuration config = null;
	private static String directory = "";				// Repertoire de travail
	private static String inputFileName = "";			// Fichier d'entree de la cle USB (1! record)
	private static String startDate = "";				// Date de debut au format YYYYMMDD HHMMSS (codage PIC)
	private static XMLGregorianCalendar xmlStartDate = null;	// Date de debut au format Xml
	private static String endDate = null;				// Date de fin au format YYYYMMDD HHMMSS (codage PIC)
	private static XMLGregorianCalendar xmlEndDate = null;	// Date de fin au format Xml
	//private static String duration = null;			// Duree => Date de fin en l'absence de celle-ci
	
	private static String splitFileName = "";			// Fichiers de sortie splites
	private static String rcdFileName = "";				// Fichier de sortie pour une plage horaire (n records valides)
	private static String xmlFileName = "";				// Fichier xml intermediaire (1! record)
	private static String gpxFileName = "";				// Fichier gpx a poster (n records)
	private static int scaleLatLonBigDecimal = 0;		// Nbr de chiffres apres la virgule (troncature)
	private static int scaleEleBigDecimal = 0;			// Nbr de chiffres apres la virgule (troncature)
	private static int nbrIndentation = 0;				// Nbr d'espaces pour l'indentation (1 espace au moins)

	private static int durationBetweenPeriods = 0;		// Duree en secondes separant 2 periodes distinctes

	private static double speedThresholdHigh = 0.0;		// Seuil haut vitesse en km/h pour une meme periode
	private static double speedThresholdLow = 0.0;		// Seuil bas vitesse en km/h pour une meme periode
	private static int speedTimeoutHigh = 0;			// Timeout passage au dessus de 'speedThresholdHigh'
	private static int speedTimeoutLow = 0;				// Timeout passage en deca de 'speedThresholdLow'

	private static int distBetweenSamples = 0;			// Nbr de metres entre echantillons de mesures => .gpx
	private static double dist2DCumul = -1.0;			// Distance precedente pour difference @ seuil 'distBetweenSamples'
	private static double dist2DTotal = 0.0;
	private static double dist3DCumul = -1.0;			// Distance precedente pour difference @ seuil 'distBetweenSamples'
	private static double dist3DTotal = 0.0;

	private static boolean flgValPrevious = false;		// Valeurs precedentes 'xxPrevious' non valides
	private static double latPrevious = 0.0;			// Valeurs d'initialisation "irrealistes"
	private static double lonPrevious = 0.0;			// => Sur l'equateur a la longitude de Greenwich
	private static double elePrevious = -9999.9;		//    et ... a 10 000 metres sous la mer

	// Variables locales
	//private static DatatypeFactory df = null;
	private static List<String> gpxRecords = new ArrayList<String>();
	private static List<String> rcdRecords = new ArrayList<String>();
	private static int nbrIndent = 0;

	private static double minLat = Double.MAX_VALUE;
	private static double minLon = Double.MAX_VALUE;
	private static double minEle = Double.MAX_VALUE;
	private static double maxLat = Double.MIN_VALUE;
	private static double maxLon = Double.MIN_VALUE;
	private static double maxEle = Double.MIN_VALUE;

	private static boolean flgSplit = false;
	private static boolean flgOutputXml = false;
	private static boolean flgOutputRecords = false;
	private static boolean flgInverse = false;

	private static List<WayPoint> cutDates = new ArrayList<WayPoint>();
	private static List<WayPoint> smoothDefinitions = new ArrayList<WayPoint>();
	private static List<WayPoint> waypoints = new ArrayList<WayPoint>();
	private static int nbrRcdSmooth = 0;
	private static int nbrRcdCut = 0;
	private static int cutDatesCurrent = -1;
	private static int cutDatesTypeTest = -1;

	// Methods
	private static String to02d(int n) { return (n < 10)      ? "0" + n        : Integer.valueOf(n).toString(); }
	private static String to03d(int n) { return (n < 100)     ? "0" + to02d(n) : Integer.valueOf(n).toString(); }
	private static String to04d(int n) { return (n < 1000)    ? "0" + to03d(n) : Integer.valueOf(n).toString(); }
	private static String to05d(int n) { return (n < 10000)   ? "0" + to04d(n) : Integer.valueOf(n).toString(); }
	private static String to06d(int n) { return (n < 100000)  ? "0" + to05d(n) : Integer.valueOf(n).toString(); }
	private static String to07d(int n) { return (n < 1000000) ? "0" + to06d(n) : Integer.valueOf(n).toString(); }
	private static String to08d(int n) { return (n < 10000000)? "0" + to07d(n) : Integer.valueOf(n).toString(); }

	private static String to2d(int n) { return (n < 10)      ? " " + n        : Integer.valueOf(n).toString(); }
	private static String to3d(int n) { return (n < 100)     ? " " + to2d(n) : Integer.valueOf(n).toString(); }
	private static String to4d(int n) { return (n < 1000)    ? " " + to3d(n) : Integer.valueOf(n).toString(); }
	private static String to5d(int n) { return (n < 10000)   ? " " + to4d(n) : Integer.valueOf(n).toString(); }
	private static String to6d(int n) { return (n < 100000)  ? " " + to5d(n) : Integer.valueOf(n).toString(); }
	private static String to7d(int n) { return (n < 1000000) ? " " + to6d(n) : Integer.valueOf(n).toString(); }
	private static String to8d(int n) { return (n < 10000000)? " " + to7d(n) : Integer.valueOf(n).toString(); }

/* For the elementary tests
	private static GpxType setGpxType(Configuration config) throws DatatypeConfigurationException {

		GpxType gpxType = new GpxType();

		// Version
		String version = config.getVersion() + "\">";
		gpxType.setVersion(version);

		// Creator
		String creatorAndExtensionsType = config.getProduct() + " - http://www.topografix.com\"";

		// Warning: Concatenation des Extensions en attendant de modifier 'ExtensionType'
		// qui ne propose pas de mutateur 'setAny(List<Object>)' (TODO a l'occasion)
		creatorAndExtensionsType += " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
		creatorAndExtensionsType += " xmlns=\"http://www.topografix.com/GPX/1/0\"";
		creatorAndExtensionsType += " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"";
		gpxType.setCreator(creatorAndExtensionsType);		

		// TBC: JAXBException [null] with the following code:
		//ExtensionsType extensionsType = new ExtensionsType();
		//String x1 = "My extension";	
		//extensionsType.getAny().add(x1);
		//gpxType.setExtensions(extensionsType);
		//

		// Metadata
		MetadataType metadataType = new MetadataType();
		PersonType personType = new PersonType();
		String name = "My name";
		personType.setName(name);
		metadataType.setAuthor(personType);

		String desc = "Localization and misc... (xx km)";
		metadataType.setDesc(desc);

		String date = "2012/01/15 10:59:10";
		metadataType.setTime(new Formater().dateToXmlCalendar(date));

		BoundsType boundsType = new BoundsType();
		BigDecimal minlat = new BigDecimal(48.1);
		boundsType.setMinlat(minlat);
		BigDecimal maxlat = new BigDecimal(48.9);
		boundsType.setMaxlat(maxlat);
		BigDecimal minlon = new BigDecimal(1.571);
		boundsType.setMinlon(minlon);
		BigDecimal maxlon = new BigDecimal(1.579);
		boundsType.setMaxlon(maxlon);
		metadataType.setBounds(boundsType);

		gpxType.setMetadata(metadataType);

		// 2 x Wpt
		WptType wptType = new WptType();
		wptType.setLat(minlat.add(new BigDecimal(0.25)));
		wptType.setLon(minlon.add(new BigDecimal(0.25)));
		wptType.setName("First waypoint");
		wptType.setDesc("Description of 1st waypoint");
		gpxType.getWpt().add(wptType);
		wptType = new WptType();
		wptType.setLat(minlat.add(new BigDecimal(0.5)));
		wptType.setLon(minlon.add(new BigDecimal(0.5)));
		wptType.setName("Second waypoint");
		wptType.setDesc("Description of 2nd waypoint");
		gpxType.getWpt().add(wptType);
		
		// Trk with name and Trkseg + 3 trkpt
		BigDecimal ele = new BigDecimal(100.1);
		TrkType trkType = new TrkType();
		trkType.setName("Traces description");
		TrksegType trksegType = new TrksegType();
		WptType trkpt = new WptType();
		trkpt.setLat(minlat.add(new BigDecimal(0.1)));
		trkpt.setLon(minlon.add(new BigDecimal(0.1)));
		trkpt.setEle(ele);

		date = "2012/01/15 10:59:26";
		trkpt.setTime(new Formater().dateToXmlCalendar(date));
		trksegType.getTrkpt().add(trkpt);

		trkpt = new WptType();
		trkpt.setLat(minlat.add(new BigDecimal(0.2)));
		trkpt.setLon(minlon.add(new BigDecimal(0.2)));
		trkpt.setEle(ele.add(new BigDecimal(1.0)));

		date = "2012/01/16 23:52:05";
		trkpt.setTime(new Formater().dateToXmlCalendar(date));
		trksegType.getTrkpt().add(trkpt);

		trkpt = new WptType();
		trkpt.setLat(minlat.add(new BigDecimal(0.3)));
		trkpt.setLon(minlon.add(new BigDecimal(0.3)));
		trkpt.setEle(ele.add(new BigDecimal(2.0)));

		date = "2012/01/17 07:30:25";
		trkpt.setTime(new Formater().dateToXmlCalendar(date));
		trksegType.getTrkpt().add(trkpt);

		trkType.getTrkseg().add(trksegType);
		gpxType.getTrk().add(trkType);

		return gpxType;
	}
*/

	private static void addGpxRecords(String line, int indent) {

		if (indent == INDENT_PLUS) {
			nbrIndent++;
		}
		if (indent == INDENT_MOINS) {
			if (nbrIndent > 0) {
				nbrIndent--;
			}
		}
		String loc_line = "";
		String loc_indent = "        ";		// 8 indentation characters max
		for (int n = 0; n < nbrIndent; n++) {
			loc_line += loc_indent.substring(0, nbrIndentation);
		}
		gpxRecords.add(loc_line + line);
	}

	private static void addGpxRecords(String line) {

		addGpxRecords(line, INDENT_NONE);
	}

	private static int writeFile(String filePathName, List<String > records) throws IOException {

		int nbrRecords = 0;
		try {
			File file = new File(filePathName);
			BufferedWriter out = new BufferedWriter(new FileWriter(file, false));

			for (int n = 0; n < records.size(); n++, nbrRecords++) {
				out.write(records.get(n));
				out.newLine();
			}

	    	out.flush();
	    	out.close();
		}
		catch (IOException e) {
			throw new IOException("Exception while obtaining FileWriter instance");
		}
		return nbrRecords;
	}

	private static void addRcdRecords(String line) {

		rcdRecords.add(line);
	}

	private static void addRcdRecords(String line, int numRecord) {

		rcdRecords.add("#" + to05d(numRecord) + " [" + line + "]");
	}

	private static List<Record> analyzeInputFile(String filePathName, long len, boolean flgSplit) throws FileNotFoundException, IOException, Exception {

		logger.info("");
		logger.info("=> " + (flgSplit == false ? "Analyzing" :"Splitting") + " [" + filePathName + "] (" + len + " bytes)...");

		try {
			BufferedReader in = new BufferedReader(new FileReader(filePathName));

			// Lecture bloc par bloc car le fichier peut etre tres gros (> 10Mb ;-)
			int posByteLoaded = 0;
			int nbrBytesUsed = 0;
			int nbrBytesSkipped = 0;
			boolean flgBeginRecord = false;
			String sRecord = "";
			long percentPrevious = 0L;
			for (int n = 0; ; n++) {
				char[] bufferIn = new char[SIZE_BUFFER_READ];;
				int nbrBytes = in.read(bufferIn, 0, SIZE_BUFFER_READ);
				if (nbrBytes == -1) {
					break;
				}
				logger.debug("   #" + to05d(n) + " Reading " + nbrBytes + " bytes");
				//String sBuffer = "";
				for (int p = 0; p < nbrBytes && p < SIZE_BUFFER_READ; p++) {
					Character c = bufferIn[p];
					if (c.compareTo(' ') >= 0 && c.compareTo('~') <= 0) {
						if (c.compareTo(Record.CHAR_START) == 0) {
							if (flgBeginRecord == true) {
								analyzeRecord(sRecord);
								sRecord = "";
							}
							flgBeginRecord = true;
						}
						//sBuffer += c;
						if (flgBeginRecord == true) {
							sRecord += c;
						}
						nbrBytesUsed++;
					}
					else {
						//logger.error("=> Invalid character [0x" +
							//new Formater().character2Hex(c) + "] at position [0x" +
							//new Formater().int2Hex(posByteLoaded) + "]");
						if (flgBeginRecord == true) {
							analyzeRecord(sRecord);
							sRecord = "";
						}
						flgBeginRecord = false;
						nbrBytesSkipped++;
					}
					posByteLoaded++;

					// Patienteur...
					long percent = (100L * posByteLoaded) / len;
					if (percent >= (percentPrevious + 10L)) {
						logger.warn(percent + "% of processing");
						percentPrevious = percent;
					}
				}
				//logger.debug("   [" + sBuffer + "]");
			}

			if (flgSplit == true) {
				logger.info("   " + to8d(new Record().getListRecordsToFilter().size()) + " records to keep for splitting (checksum and datas valid): ");
				String datePrevious = "";
				List<String> records = null;
				String splitFilePath = "";
				int n = 0;
				for (Record record: new Record().getListRecordsToFilter()) {
					String locSDate = record.getSDate();
					if (datePrevious.equals(locSDate) == false) {
						if (records != null) {
							writeFile(splitFilePath, records);
						}
						// Ajout suffixe YYYYMMDD construit a partir de 'locSDate' = DDMMYY
						String suffix = "20" + locSDate.substring(4, 6) + locSDate.substring(2, 4) + locSDate.substring(0, 2);

						// Test d'ecrassement potentiel du fichier d'entree
						if (inputFileName.equals(splitFileName + suffix) == true) {
							throw new DatatypeConfigurationException("Potential ecrasement of the input file [" + inputFileName + "]");
						}
						splitFilePath = directory + File.separatorChar + splitFileName + suffix;
						logger.info("            #" + to03d(n++) + " Time [" + record.getXDateAndTime() +  "] => [" + splitFilePath + "]");						
						datePrevious = record.getSDate();
						records = new ArrayList<String>();
					}
					records.add(record.getSRecord());
				}
				if (records != null) {
					writeFile(splitFilePath, records);
				}
				return null;	// Fin prematuree si "splitting"
			}

			logger.info("");
			logger.info("=> Statistics:");
			logger.info("   " + to8d(posByteLoaded) + " bytes loaded");
			logger.info("   " + to8d(nbrBytesUsed) + " bytes used");
			logger.info("   " + to8d(nbrBytesSkipped) + " bytes skipped (not in the range [' ', ..., '~'])");
			logger.info("   " + to8d(new Record().getNbrRecords()) + " records analyzed");
			logger.info("   " + to8d(new Record().getNbrRecordsOk()) + " valid records analyzed");
			logger.info("   " + to8d(new Record().getNbrRecordsKo()) + " invalid records analyzed");
			logger.info("   " + to8d(new Record().getNbrRecordsBadCks()) + " including records with a wrong checksum");
			logger.info("");
			logger.info("   " + to8d(1 + new Record().getListRecordsToIgnore().size()) + " records to ignore (including the 1st and last record):");
			for (Record record: new Record().getListRecordsToIgnore()) {
				logger.debug("            #" + to05d(record.getRecordId()) + " [" + record.getSRecord() + "]");
			}
			Record recordLast = new Record().getRecordCurrent();
			logger.debug("            #" + to05d(recordLast.getRecordId()) + " [" + recordLast.getSRecord() + "]");			
			logger.debug("");
			logger.info("   " + to8d(new Record().getListRecordsInvalid().size()) + " records invalid (incomplete or invalid datas): ");
			for (Record record: new Record().getListRecordsInvalid()) {
				logger.debug("            #" + to05d(record.getRecordId()) + " [" + record.getSRecord() + "]");
			}
			logger.debug("");
			logger.info("   " + to8d(new Record().getListRecordsToFilter().size()) + " records to keep before filtering (checksum and datas valid): ");
			for (Record record: new Record().getListRecordsToFilter()) {
				logger.debug("            #" + to05d(record.getRecordId()) + " [" + record.getSRecord() + "]");
				logger.debug("Time [" + record.getXDateAndTime() + "] Lat [" + record.getXLat() + "] Lon [" + record.getXLon() + "] Ele [" + record.getXEle() + "]");
				logger.debug("Speed [" + record.getSpeedKmh() + "] km/h");
			}
			// Verification ;-)
			int nbrRecords =
				(1 + new Record().getListRecordsToIgnore().size()) +
				new Record().getListRecordsInvalid().size() +
				new Record().getListRecordsToFilter().size();
			logger.debug("");
			if (new Record().getNbrRecords() == nbrRecords) {
				logger.info("            => Verif. " + new Record().getNbrRecords() + " == (" +
						(1 + new Record().getListRecordsToIgnore().size()) + " + " + 
						new Record().getListRecordsInvalid().size() + " + " +
						new Record().getListRecordsToFilter().size() + ")");
			}
			else {
				logger.error("            => Verif. " + new Record().getNbrRecords() + " != (" +
						(1 + new Record().getListRecordsToIgnore().size()) + " + " + 
						new Record().getListRecordsInvalid().size() + " + " +
						new Record().getListRecordsToFilter().size() + ")");				
			}

			logger.info("");
			logger.info("=> Determination of separate periods of at least " + durationBetweenPeriods + " seconds (" +
					new Formater().duration(durationBetweenPeriods) + ")...");
			new Period().resetProperties();
			new Period().setDurationBetweenPeriods(durationBetweenPeriods);
			for (Record record: new Record().getListRecordsToFilter()) {
				new Period(record.getTime());
			}
			new Period(Long.MAX_VALUE);		// Pour terminer la derniere periode
			List<Period> periods = new Period().getListPeriodsCriteriaDuration();
			int nbrSamples = 0;
			int nbrSamplesExpected = new Record().getListRecordsToFilter().size();
			long durationTotal = 0;
			logger.info("=> " + periods.size() + " periods @time found:");
			for (Period period: periods) {
				XMLGregorianCalendar loc_xmlStartDate = new Formater().dateToXmlCalendar(period.getBeginningTime());
				XMLGregorianCalendar loc_xmlEndDate = new Formater().dateToXmlCalendar(period.getEndingTime());
				logger.info("   #" + period.getId() +
					" Beginning [" + loc_xmlStartDate +
					"] Ending [" + loc_xmlEndDate +
					"] Duration [" + new Formater().duration(period.getDuration()) +
					"] Samples [" + period.getNbrSamples() + "]");

				nbrSamples += period.getNbrSamples();
				durationTotal += period.getDuration();
			}

			logger.info("");
			// Duree totale des periodes
			logger.info("   => Total duration [" + new Formater().duration(durationTotal) + "]");
			// Verification ;-)
			if (nbrSamples == nbrSamplesExpected) {
				logger.info("   => Verif. " + nbrSamples + " == " + nbrSamplesExpected);
			}
			else {
				logger.error("   => Verif. " + nbrSamples + " != " + nbrSamplesExpected);
			}

			logger.info("");
			logger.info("=> Determination of periods @speed: Threshold low/high [" +
				speedThresholdLow + "]/[" + speedThresholdHigh + "] km/h...");
			int numRecord = 0;
			int numRecord2 = 0;		// numRecord without offset (#0 at begin of the period)
			int numPeriod = 0;
			int numSampleFrom = 0;
			int numSampleTo = periods.get(numPeriod).getNbrSamples();
			new Period().setSpeedProperties(speedThresholdHigh, speedThresholdLow, speedTimeoutHigh, speedTimeoutLow);
			// Last date of records
			long lastTime = 0L;
			for (Record record: new Record().getListRecordsToFilter()) {
				// Determination de la periode d'appartenance
				if (numRecord == 0 || numRecord == numSampleTo) {
					if (numRecord > 0) {
						numPeriod++;
						numSampleFrom = numSampleTo;
						numSampleTo = numSampleFrom + periods.get(numPeriod).getNbrSamples();
					}
					logger.debug("=> Period @time #" + to2d(numPeriod) +
							" From [" + to5d(numSampleFrom) + "] To [" + to5d(numSampleTo - 1) + "]");
					new Period().resetProperties();
				}

				// Trace debut / fin de periode @temps
				if (numRecord == numSampleFrom || numRecord == (numSampleTo - 1)) {
					logger.debug("#" + to5d(numRecord) + " => #" + to5d(numRecord2) + " => In period @time #" + numPeriod);
				}
				// Periodes @vitesse dans cette periode @ duration
				new Period(record.getTime(), record.getSpeedKmh());
				numRecord2 = (numRecord == (numSampleTo - 1)) ? 0 : (numRecord2 + 1);
				numRecord++;

				lastTime = record.getTime();
			}
			new Period(lastTime, null);		// Pour terminer la derniere periode si celle-ci est commencee

			periods = new Period().getListPeriodsCriteriaSpeed();
			logger.info("=> " + periods.size() + " periods @speed found:");
			for (Period period: periods) {
				XMLGregorianCalendar loc_xmlStartDate = new Formater().dateToXmlCalendar(period.getBeginningTime());
				XMLGregorianCalendar loc_xmlEndDate = new Formater().dateToXmlCalendar(period.getEndingTime());
				logger.info("   #" + period.getId() +
					" Beginning [" + loc_xmlStartDate +
					"] Ending [" + loc_xmlEndDate +
					"] Duration [" + new Formater().duration(period.getDuration()) +
					"] Samples [" + period.getNbrSamples() +
					"] Speed average [" + new Formater().doubleToString(period.getSpeedAverage(), 1) + " km/h]");
			}

			long duration = (xmlEndDate.toGregorianCalendar().getTimeInMillis() - xmlStartDate.toGregorianCalendar().getTimeInMillis());
			logger.info("");
			logger.info("=> " + new Record().getListRecordsToFilter().size() + " records to filter");
			logger.info("=> Determination of period after filtering from [" +
				xmlStartDate + "] to [" + xmlEndDate + "] (" + new Formater().duration(duration) + ")");
			numRecord = 0;
			List<Record> records = new ArrayList<Record>();
			numRecord = 0;
			for (Record record: new Record().getListRecordsToFilter()) {
				// Extraction @ la periode [startDate, ..., endDate] + parametres de filtrage
				// Les methodes 'filteredByXxx()' retournent 'true' si le record est a conserver
				if (filteredByDates(numRecord, record) == true) {
					record.setFlgNewTrkt(false);	// A priori, pas de nouveau 'trkt'

					// Maj lat/lon des waypoints @ date du record
					// Remarque: Un waypoint "pose" sur un record qui sera supprime, sera quand meme presente
					updateWaypoints(numRecord, record);

					// Passage dans le filtre de 'cut'
					if (filteredByCut(numRecord, record) == true) {
						if (record.getFlgNewTrkt() == true) {
							// Le record qui commence un nouveau 'trkt' n'est jamais "lisse" pour ne pas etre supprime
							records.add(record);
						}
						else if (filteredBySmooth(record) == true) {	// Passage dans le filtre de 'lissage'
							records.add(record);
						}
					}
				}
				if (records.size() != 0) {
					numRecord++;
				}
			}

			logger.info("");
			logger.info("=> List of the " + records.size() + " records after filtering: ");
			// Min. / Max. values
			Record lastRecord = null;
			for (Record record: records) {
				logger.debug("   #" + to05d(record.getIdAfterFiltering()) +
						" Time [" + record.getXDateAndTime() +
						"] Lat [" + new Formater().bigDecimalToString(record.getXLat(), 6) +
						"] Lon [" + new Formater().bigDecimalToString(record.getXLon(), 6) +
						"] Ele [" + new Formater().bigDecimalToString(record.getXEle(), 1) +
						"] Speed [" + new Formater().doubleToString(record.getSpeedKmh(), 1) + " km/h]" +
						" Dist. enrg. [" + new Formater().doubleToString(record.getDistEnrg(), 1) + " km]" +
						" Dist. 2D total [" + new Formater().doubleToString(record.getDist2DTotal(), 3) + " km]" +
						" Dist. 3D total [" + new Formater().doubleToString(record.getDist3DTotal(), 3) + " km]");

				minLat = setMinValues(minLat, record.getXLat().doubleValue());
				minLon = setMinValues(minLon, record.getXLon().doubleValue());
				minEle = setMinValues(minEle, record.getXEle().doubleValue());

				maxLat = setMaxValues(maxLat, record.getXLat().doubleValue());
				maxLon = setMaxValues(maxLon, record.getXLon().doubleValue());
				maxEle = setMaxValues(maxEle, record.getXEle().doubleValue());

				lastRecord = record;
			}
			// Maj des min/max @ aux eventuels 'waypoints'
			for (WayPoint waypoint: config.getWaypoints()) {
				double lat = waypoint.getLat();
				double lon = waypoint.getLon();
				if (Double.isNaN(lat) == false && Double.isNaN(lon) == false) {
					minLat = setMinValues(minLat, lat);
					minLon = setMinValues(minLon, lon);
					maxLat = setMaxValues(maxLat, lat);
					maxLon = setMaxValues(maxLon, lon);					
				}
			}

			// Traces etendues
			LoggerExtend loggerExtend = new LoggerExtend();

			loggerExtend.info("");
			loggerExtend.info("Synthesys (Min. / Max. and others values)");
			double correction = Math.cos(2.0 * Math.PI * ((minLat + maxLat) / 2.0) / 360.0);
			double distLat = 60.0 * 1852.0 * (maxLat - minLat);
			double distLon = 60.0 * 1852.0 * (maxLon - minLon) * correction;
			Record record = lastRecord;
			loggerExtend.info("   Start date [" + xmlStartDate + "]");
			loggerExtend.info("   Stop  date [" + xmlEndDate + "]");
			loggerExtend.info("   Duration [" + new Formater().duration(duration) + "]");
			loggerExtend.info("   Nbr of records [" + records.size() + "]");
			loggerExtend.info("   Nbr records smooth [" + nbrRcdSmooth + "]");
			loggerExtend.info("   Nbr records cut [" + nbrRcdCut + "]");
			loggerExtend.info("   Nbr waypoints [" + waypoints.size() + "]");
			loggerExtend.info("   Dist. 2D total [" + new Formater().doubleToString(record.getDist2DTotal(), 3) + " km]" +
				" Dist. 3D total [" + new Formater().doubleToString(record.getDist3DTotal(), 3) + " km]");
			loggerExtend.info("   Lat Min. [" + new Formater().doubleToString(minLat, 6) +
				"] Max. [" + new Formater().doubleToString(maxLat, 6) +
				"] Dist. [" + new Formater().doubleToString(distLat/1000.0, 3) + " km]");
			loggerExtend.info("   Lon Min. [" + new Formater().doubleToString(minLon, 6) +
				"] Max. [" + new Formater().doubleToString(maxLon, 6) +
				"] Dist. [" + new Formater().doubleToString(distLon /1000.0, 3) + " km]");
			loggerExtend.info("   Ele Min. [" + new Formater().doubleToString(minEle, 1) +
				"] Max. [" + new Formater().doubleToString(maxEle, 1) +
				" m] Diff. [" + new Formater().doubleToString((maxEle - minEle), 1) + " m]");
			loggerExtend.info("   Denivele Negatif [" + new Formater().doubleToString(record.getDeniveleNeg(), 1) +
				" m] Positif [" + new Formater().doubleToString(record.getDenivelePos(), 1) + " m]");

			in.close();
			return records;
		}
		catch (FileNotFoundException e) {
			throw new FileNotFoundException("[" + filePathName + "]: No such file");
		}
		catch (IOException e) {
			throw new IOException("IOException (FileReader, read, ...) [" + e.getMessage() + "]");
		}
		catch (Exception e) {
			//throw new IOException("Exception (Record, ...)", e);
			throw new IOException("Exception (Record, ...) [" + e.getMessage() + "]");
		}
	}

	private static void analyzeRecord(String sRecord) throws Exception {

		//logger.info("analyzeRecord(" + sRecord + ")");
		int nbrRecord = new Record().getNbrRecords();
		Record record = new Record(sRecord);
		boolean flgRecordValid = record.isValidRecord();
		logger.debug("      #" + to05d(nbrRecord) + " [" +
				(flgRecordValid == true ? "OK" : "KO") + "] [" + sRecord + "]");
		logger.debug("");
	}

	private static boolean filteredByDates(int numRecord, Record record) {

		XMLGregorianCalendar xmlDate = record.getXDateAndTime();

		boolean flgFiltering = 
		   ((xmlDate.compare(xmlStartDate) == DatatypeConstants.GREATER || xmlDate.compare(xmlStartDate) == DatatypeConstants.EQUAL)
		 && (xmlDate.compare(xmlEndDate) == DatatypeConstants.LESSER || xmlDate.compare(xmlEndDate) == DatatypeConstants.EQUAL));

		// Calcul de la distance horizontale
		// 1.0 degre de deplacement correspond a (60 * 1852) metres parcourus
		// Les deplacements le long d'une parallele (delta lon) doivent etre corriges de cos((lat + latPrevious) / 2)
		// => Warning: cos(radians) avec (x radians) = (2 * PI * x degres / 360.0 ;-)
		// De plus, le deplacement en altitude participe au deplacement total (Pytharore dans l'espace ;-)
		// => Remarque: La prise en compte des dÃ©placements en altitude produit une importante variation
		//              sur la distance parcourue (> 10%) ?!.
		//              => Peut-etre due a la precision (TBC @ a la distance donnee par le site de publication)
		boolean flgDistance = false;
		double loc_lat = 0.0;
		double loc_lon = 0.0;
		double loc_ele = 0.0;
		if (flgFiltering == true) {
			loc_lat = record.getXLat().doubleValue();
			loc_lon = record.getXLon().doubleValue();
			loc_ele = record.getXEle().doubleValue();

			// Valeurs d'initialisation non realistes pour forcer de filter le 1st record #0
			// Remarque: Pas d'egalite stricte avec des doubles ;-);
			if (flgValPrevious == false) {
				flgDistance = true;
				flgValPrevious = true;
			}
			else {
				double correction = Math.cos(2.0 * Math.PI * ((loc_lat + latPrevious) / 2.0) / 360.0);
				double distLat = 60.0 * 1852.0 * (loc_lat - latPrevious);
				double distLon = 60.0 * 1852.0 * (loc_lon - lonPrevious) * correction;
				double dist2D = Math.sqrt((distLat * distLat) + (distLon * distLon));	// Distance horizontale

				// Calcul avec deplacement vertical
				double distEle = (loc_ele - elePrevious);
				double dist3D = Math.sqrt((dist2D * dist2D) + (distEle * distEle));		// + Distance verticale
				dist2DCumul += dist2D;
				dist3DCumul += dist3D;
				if (dist2DCumul >= (1.0 * distBetweenSamples)) {
					dist2DTotal += (dist2DCumul / 1000.0);	// Distance horizontale totale depuis le 1st record en km
					dist3DTotal += (dist3DCumul / 1000.0);	// Distance horiz. + vert. totale depuis le 1st record en km

					record.setIdAfterFiltering(numRecord);
					record.setDist2DTotal(dist2DTotal);
					record.setDist3DTotal(dist3DTotal);

					if (distEle < 0.0) {
						double deniveleNeg = record.getDeniveleNeg() - distEle;
						record.setDeniveleNeg(deniveleNeg);
					}
					else if (distEle > 0.0) {					
						double denivelePos = record.getDenivelePos() + distEle;
						record.setDenivelePos(denivelePos);
					}
					
					flgDistance = true;
				}
			}

			if (flgDistance == true) {
				logger.debug("   #" + to05d(numRecord) +
					" Time [" + xmlDate +
					"] Lat [" + new Formater().bigDecimalToString(record.getXLat(), 6) +
					"] Lon [" + new Formater().bigDecimalToString(record.getXLon(), 6) +
					"] Ele [" + new Formater().bigDecimalToString(record.getXEle(), 1) +
					"] Speed [" + new Formater().doubleToString(record.getSpeedKmh(), 1) + " km/h]" +
					" Dist. enrg. [" + new Formater().doubleToString(record.getDistEnrg(), 3) + " km]" +
					" Dist. 2D total [" + new Formater().doubleToString(record.getDist2DTotal(), 3) + " km]" +
					" Dist. 3D total [" + new Formater().doubleToString(record.getDist3DTotal(), 3) + " km]");

				dist2DCumul = 0.0;
				dist3DCumul = 0.0;
			}
		}

		latPrevious = loc_lat;
		lonPrevious = loc_lon;
		elePrevious = loc_ele;

		return (flgFiltering && flgDistance);
	}

	private static boolean filteredBySmooth(Record record) {

		for (int n = 0; n < smoothDefinitions.size(); n++) {
			WayPoint dates = smoothDefinitions.get(n);
			if (dates.isBetweenDates(record.getXDateAndTime()) == true) {
				logger.debug("filteredBySmooth(): #" + nbrRcdSmooth + " Record suppressed with Date #" + n + " [" + record.getXDateAndTime() + "]");
				nbrRcdSmooth++;
				return false;
			}
			if (dates.isBetweenCornerLatLon(record.getXLat(), record.getXLon()) == true) {
				logger.debug("filteredBySmooth(): #" + nbrRcdSmooth + " Record suppressed with Corner #" + n + " [" + record.getXLat() + "]/[" + record.getXLon() + "]");
				nbrRcdSmooth++;
				return false;
			}
			if (dates.isBetweenFromToLatLon(record.getXLat(), record.getXLon()) == true) {
				logger.info("filteredBySmooth(): #" + nbrRcdSmooth + " Record suppressed with From/To #" + n + " [" + record.getXLat() + "]/[" + record.getXLon() + "]");
				nbrRcdSmooth++;
				return false;
			}
		}
		return true;
	}

	private static boolean filteredByCut(int numRecord, Record record) throws Exception {

		final int CUT_DATES_START = 0, CUT_DATES_END = 1;

		if (cutDates.size() == 0) {
			return true;	// Aucune plage a analyser
		}

		// Prise du couple de dates courant
		if (cutDatesCurrent >= cutDates.size()) {
			return true;	// Toutes les plages ont ete analysees
		}

		if (numRecord == 0) {
			// Inialisation de l'automate et debut du 1st 'trkt'
			cutDatesCurrent = 0;
			cutDatesTypeTest = CUT_DATES_START;

			// Renseignement du 1st 'trkt'
			record.setFlgNewTrkt(true);
			record.setTrktName(config.getTrackpointName());
			return true;
		}

		WayPoint dates = cutDates.get(cutDatesCurrent);

		// Suivant l'automate de progression, test de la date du record @ debut/fin de la plage horaire
		// pour terminer la precedente plage et commencer une nouvelle plage (cf. 'trkt' du .gpx) avec
		// son "petit" nom (attribut <name>"
		switch (cutDatesTypeTest) {
		case CUT_DATES_START:
			if (new Formater().compareDates(record.getXDateAndTime(), dates.getDateFrom()) == DatatypeConstants.GREATER) {
				// La date du record fait passer dans la plage d'analyse en cours
				cutDatesTypeTest = CUT_DATES_END;
			}
			return true;			// Conservation des records et du record qui fait rentrer dans la plage

		case CUT_DATES_END:
			if (new Formater().compareDates(record.getXDateAndTime(), dates.getDateTo()) == DatatypeConstants.GREATER) {
				// La date du record fait sortir de la plage d'analyse en cours
				cutDatesTypeTest = CUT_DATES_START;
				cutDatesCurrent++;	// Passage a la plage suivante

				// Renseignement du nouveau 'trkt'
				record.setFlgNewTrkt(true);
				record.setTrktName(dates.getDescription());
				return true;		// Conservation du record qui fait sortir de la plage
			}
			nbrRcdCut++;
			return false;			// Suppression des records tant que la date de fin n'est pas atteinte

		default:
			throw new Exception("filteredByCut(): Internal error (unknown cutDatesTypeTest [" + cutDatesTypeTest + "])");
		}
	}

	private static void updateWaypoints(int numRecord, Record record) {

		LoggerExtend loggerExtend = new LoggerExtend();

		if (numRecord == 0 && waypoints.size() != 0) {
			int n = 0;
			for (WayPoint waypoint: waypoints) {
				if (Double.isNaN(waypoint.getLat()) == true || Double.isNaN(waypoint.getLon()) == true) {
					n++;
				}
			}
			//logger.info("");
			//logger.info("=> Update the " + n + "/" + waypoints.size() + " waypoints");

			//loggerExtend.info("");
			//loggerExtend.info("List of the " + waypoints.size() + " waypoints updated");
		}
		for (WayPoint waypoint: waypoints) {
			if (Double.isNaN(waypoint.getLat()) == true || Double.isNaN(waypoint.getLon()) == true) {
				if (new Formater().compareDates(record.getXDateAndTime(), waypoint.getDateFrom()) == DatatypeConstants.GREATER) {
					waypoint.setLat(record.getXLat().doubleValue());
					waypoint.setLon(record.getXLon().doubleValue());
					loggerExtend.info("   Name [" + waypoint.getName() + "] Desc. [" + waypoint.getDescription() +
						"] Time [" + waypoint.getDateFrom() +
						"] Lat [" + waypoint.getLat() + "] Lon [" + waypoint.getLon() + "]");
					waypoint.setFlgUpdate(true);
					record.setWaypointInfos("[" + waypoint.getName() + "] [" + waypoint.getDescription() + "]");
					break;
				}
			}
			else if (waypoint.getFlgUpdate() == false) {
				loggerExtend.info("   Name [" + waypoint.getName() + "] Desc. [" + waypoint.getDescription() +
						"] Time [" + waypoint.getDateFrom() +
						"] Lat [" + waypoint.getLat() + "] Lon [" + waypoint.getLon() + "]");
				waypoint.setFlgUpdate(true);
			}
		}
	}

	private static double setMinValues(double min, double value) { return (value < min) ? value : min; }
	private static double setMaxValues(double max, double value) { return (value > max) ? value : max; }

	public static void main(String args[]) throws JAXBException, FileNotFoundException, DatatypeConfigurationException, XPathExpressionException, Exception {

		Exception exception = null;
		try {
			if (args.length != 0 && args.length != 2) {
				System.out.println("Usage: " + Main.class.getName() + " [<config_dir> <config_file>]");
				System.out.println("with   config_dir:  Configuration directory (by default '" + configPath + "')");
				System.out.println("       config_file: Configuration file (by default '" + configFile + "')");
				System.exit(2);
			}

			logger.info("Start of '" + Main.class.getName() + "' with the following parameters:");
			logger.info("CLASSPATH [" + System.getProperty("java.class.path", ".") + "]");
			logger.info("");

			if (args.length == 2) {
				configPath = args[0];
				configFile = args[1];
			}
			config = new Configuration(configPath, configFile);

			directory = config.getDirectory();
			inputFileName = config.getInputFileName();
			String inputFilePathName = (directory + File.separatorChar + inputFileName);
			File inputFile = new File(inputFilePathName);

			flgSplit = config.getFlgSplit();

			if (flgSplit == true) {				
				splitFileName = config.getSplitFileName();

				// Split input file
				try {
					analyzeInputFile(inputFilePathName, inputFile.length(), true);
				} catch (Exception e) {
					logger.error("Invalid record [" + new Record().getSLastRecord() + "]");
					throw e;
				}

				logger.info("");
				logger.info("End of '" + Main.class.getName() + "'");
				System.exit(0);
			}

			scaleLatLonBigDecimal = config.getScaleLatLonBigDecimal();
			scaleEleBigDecimal = config.getScaleEleBigDecimal();
			nbrIndentation = config.getNbrIndentation();
			durationBetweenPeriods = config.getDurationBetweenPeriods();
			speedThresholdHigh = config.getSpeedThresholdHigh();
			speedThresholdLow = config.getSpeedThresholdLow();
			speedTimeoutHigh = config.getSpeedTimeoutHigh();
			speedTimeoutLow = config.getSpeedTimeoutLow();
			startDate = config.getStartDate();
			xmlStartDate = config.getXmlStartDate();
			endDate = config.getEndDate();
			xmlEndDate = config.getXmlEndDate();
			distBetweenSamples = config.getDistBetweenSamples();
			flgOutputXml = config.getFlgOutputXml();
			flgOutputRecords = config.getFlgOutputRecords();
			flgInverse = config.getFlgInverse();
			xmlFileName = (flgOutputXml == true) ? config.getXmlFileName() : "none";
			rcdFileName = (flgOutputRecords == true) ? config.getRecordsFileName() : "none";
			gpxFileName = config.getGpxFileName();
			smoothDefinitions = config.getSmoothDefinitions();
			cutDates = config.getCutDates();
			waypoints = config.getWaypoints();

			// Waypoints + plages horaires coupees et lissees (definitions optionnelles)
			config.getAndTestTimeRanges();

			// Analyzing input file		
			List<Record> records = analyzeInputFile(inputFilePathName, inputFile.length(), flgSplit);

			if (flgOutputRecords == true && flgSplit == false) {
				String rcdFilePathName = (directory + File.separatorChar + rcdFileName);
				logger.info("");
				logger.info("=> Write records to [" + rcdFilePathName + "]...");

				addRcdRecords("Begin of '" + rcdFilePathName + "'");
				addRcdRecords("");
				addRcdRecords("CLASSPATH [" + System.getProperty("java.class.path", ".") + "]");
				addRcdRecords("");
				LoggerExtend loggerExtend = new LoggerExtend();
				for (String line: loggerExtend.getMessages()) {
					addRcdRecords(line);
				}

				addRcdRecords("");
				addRcdRecords("List of " + records.size() + " records");
				for (Record record: records) {
					addRcdRecords(record.getSRecord(), record.getIdAfterFiltering());
				}
				addRcdRecords("");
				addRcdRecords("End of '" + rcdFilePathName + "'");

				int nbrRecords = writeFile(rcdFilePathName, rcdRecords);
				logger.info("=> " + nbrRecords + " records written");
			}
			
			JAXBContext jc = JAXBContext.newInstance(CommandResponseData.class);

			// Building .gpx file
			String gpxFilePathName = (directory + File.separatorChar + gpxFileName);
			logger.info("");
			logger.info("=> Building to [" + gpxFilePathName + "]...");
			CommandResponseData commandResponseData = new CommandResponseData();
			Xml xml = new Xml(config);
			xml.setMetadata(minLat, maxLat, minLon, maxLon);

			for (WayPoint waypoint: waypoints) {
				xml.addWayPoint(waypoint.getName(), waypoint.getDescription(), waypoint.getLat(), waypoint.getLon());
			}

			if (flgInverse == true) {
				long startDate = records.get(0).getTime();
				long endDate = records.get(records.size() - 1).getTime();
				long duration = (endDate - startDate);
				logger.warn("Inversion of the trace ;-)");
				logger.info("   - Start date  [" + startDate + "] [" + new Formater().dateToXmlCalendar(startDate) + "]");
				logger.info("   - End   date  [" + endDate + "] [" + new Formater().dateToXmlCalendar(endDate) + "]");
				logger.info("   - Duration    [" + (duration / 1000) + "] Sec. (" + new Formater().duration(duration) + ")");
				logger.info("   - Distance 2D [" + new Formater().doubleToString(dist2DTotal, 1) + "] Km");
				logger.info("   - Distance 3D [" + new Formater().doubleToString(dist3DTotal, 1) + "] Km");
				logger.info("");

				for (int n = (records.size() - 1); n >= 0; n--) {
					Record record = records.get(n);
					if (n > (records.size() - 10)) {
						logger.debug("#" + n + " [" + new Formater().dateToXmlCalendar(startDate + endDate - record.getTime()) + "]");
					}
					XMLGregorianCalendar dateAndTime = new Formater().dateToXmlCalendar(startDate + endDate - record.getTime());

					// Construction d'une ligne commentaire regroupant:
					// - Le #Id du record apres filtrage
					// - La distance parcourue en km
					// - La vitesse en km/h
					// - le cap en degres decimal
					String comment = "#" + record.getIdAfterFiltering() +
						" | " + new Formater().doubleToString(dist2DTotal - record.getDist2DTotal(), 1) + "km" + // Complement Distance
						" | " + new Formater().doubleToString(record.getSpeedKmh(), 1) + "km/h" +
						" | " + new Formater().doubleToString(360.0 - record.getCap(), 1) + "°";	// Complement Cap
					if (record.getWaypointInfos() != null) {
						comment += " | " + record.getWaypointInfos();
					}

					if (record.getFlgNewTrkt() == true) {
						logger.debug("#" + record.getIdAfterFiltering() + " New Trackpoint [" + record.getTrktName() + "]");
						xml.createTrackPoint(record.getTrktName());
					}

					xml.addTrackPoint(record.getXLat(), record.getXLon(),
						record.getXEle(), dateAndTime, comment);
				}
			}
			else {
				for (Record record: records) {
					// Construction d'une ligne commentaire regroupant:
					// - Le #Id du record apres filtrage
					// - La distance parcourue en km
					// - La vitesse en km/h
					// - le cap en degres decimal
					String comment = "#" + record.getIdAfterFiltering() +
						" | " + new Formater().doubleToString(record.getDist2DTotal(), 1) + "km" +
						" | " + new Formater().doubleToString(record.getSpeedKmh(), 1) + "km/h" +
						" | " + new Formater().doubleToString(record.getCap(), 1) + "°";
					if (record.getWaypointInfos() != null) {
						comment += " | " + record.getWaypointInfos();
					}

					if (record.getFlgNewTrkt() == true) {
						logger.debug("#" + record.getIdAfterFiltering() + " New Trackpoint [" + record.getTrktName() + "]");
						xml.createTrackPoint(record.getTrktName());
					}

					xml.addTrackPoint(record.getXLat(), record.getXLon(),
						record.getXEle(), record.getXDateAndTime(), comment);
				}
			}
			GpxType gpxType = xml.getType();	// setGpxType(config);
			if (gpxType == null) {
				throw new Exception("Abort...");
			}
			else {
				commandResponseData.setGpx(gpxType);
			}

			logger.info("");
			String xmlFilePathName = (directory + File.separatorChar + xmlFileName);
			Marshaller marchaller = jc.createMarshaller();
			ByteArrayOutputStream out = null;
			if (flgOutputXml == true) {
				logger.info("=> Marshaling to [" + xmlFilePathName + "]...");
				marchaller.marshal(commandResponseData, new java.io.FileOutputStream(xmlFilePathName));
			}
			else {
				out = new ByteArrayOutputStream();
				marchaller.marshal(commandResponseData, out);
				logger.info("=> Marshaling to <stream: " + out.size() + " bytes>...");
			}

			// Analyzing .xml | stream => .gpx file
			logger.debug("");
			Unmarshaller umarchaller = jc.createUnmarshaller();
			CommandResponseData response = null;
			if (flgOutputXml == true) {
				logger.info("=> Unmarshaling from [" + xmlFilePathName + "]...");
				response = (CommandResponseData)umarchaller.unmarshal(new java.io.FileInputStream(xmlFilePathName));
			}
			else {
				logger.info("=> Unmarshaling from <stream: " + out.size() + " bytes>...");
				ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
				response = (CommandResponseData)umarchaller.unmarshal(in);
			}

			addGpxRecords("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");

			gpxType = response.getGpx();
			logger.debug("=> List of <gpx ...");
			logger.debug("      version [" + gpxType.getVersion() + "]");
			logger.debug("      creator [" + gpxType.getCreator() + "]");
			addGpxRecords("<gpx creator=\"" + gpxType.getCreator() + " version=\"" + gpxType.getVersion());

			addGpxRecords("<metadata>", INDENT_PLUS);
			logger.debug("      author  [" + gpxType.getMetadata().getAuthor().getName().replace("&", "&amp;") + "]");
			addGpxRecords("<author>" + gpxType.getMetadata().getAuthor().getName() + "</author>", INDENT_PLUS);
			logger.debug("      desc    [" + gpxType.getMetadata().getDesc().replace("&", "&amp;") + "]");
			addGpxRecords("<desc>" + gpxType.getMetadata().getDesc() + "</desc>");
			logger.debug("      time    [" + gpxType.getMetadata().getTime() + "]");
			addGpxRecords("<time>" + gpxType.getMetadata().getTime() + "</time>");
			logger.debug("      min lat [" + gpxType.getMetadata().getBounds().getMinlat() +
				"] [" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMinlat(), scaleLatLonBigDecimal) + "]");
			logger.debug("      max lat [" + gpxType.getMetadata().getBounds().getMaxlat() +
				"] [" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMaxlat(), scaleLatLonBigDecimal) + "]");
			logger.debug("      min lon [" + gpxType.getMetadata().getBounds().getMinlon() +
				"] [" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMinlon(), scaleLatLonBigDecimal) + "]");
			logger.debug("      max lon [" + gpxType.getMetadata().getBounds().getMaxlon() +
				"] [" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMaxlon(), scaleLatLonBigDecimal) + "]");
			addGpxRecords("<bounds minlat=\"" +
				new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMinlat(), scaleLatLonBigDecimal) +
				"\" maxlat=\"" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMaxlat(), scaleLatLonBigDecimal) +
				"\" minlon=\"" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMinlon(), scaleLatLonBigDecimal) +
				"\" maxlon=\"" + new Formater().bigDecimalToString(gpxType.getMetadata().getBounds().getMaxlon(), scaleLatLonBigDecimal) +
				"\"/>");
			addGpxRecords("</metadata>", INDENT_MOINS);			

			List<WptType> wptTypes = gpxType.getWpt();
			logger.debug("      List of " + wptTypes.size() + " <wpt...");
			for (int n = 0; n < wptTypes.size(); n++) {
				if (n > 0) {
					logger.debug("");
				}
				logger.debug("         #" + n + " lat  [" + wptTypes.get(n).getLat() +
					"] [" + new Formater().bigDecimalToString(wptTypes.get(n).getLat(), scaleLatLonBigDecimal) + "]");
				logger.debug("         #" + n + " lon  [" + wptTypes.get(n).getLon() +
					"] [" + new Formater().bigDecimalToString(wptTypes.get(n).getLon(), scaleLatLonBigDecimal) + "]");
				logger.debug("         #" + n + " name [" + wptTypes.get(n).getName() + "]");
				logger.debug("         #" + n + " desc [" + wptTypes.get(n).getDesc() + "]");
				addGpxRecords("<wpt lat=\"" +
					new Formater().bigDecimalToString(wptTypes.get(n).getLat(), scaleLatLonBigDecimal) +
					"\" lon=\"" + new Formater().bigDecimalToString(wptTypes.get(n).getLon(), scaleLatLonBigDecimal) +
					"\">");
				addGpxRecords("<name>" + wptTypes.get(n).getName().replace("&", "&amp;") + "</name>", INDENT_PLUS);
				addGpxRecords("<desc>" + wptTypes.get(n).getDesc().replace("&", "&amp;") + "</desc>");
				addGpxRecords("</wpt>", INDENT_MOINS);
			}

			List<TrkType> trkTypes = gpxType.getTrk();
			logger.debug("      List of " + trkTypes.size() + " <trk...");
			for (int n = 0; n < trkTypes.size(); n++) {
				if (n > 0) {
					logger.debug("");
				}
				logger.debug("         #" + n + " name  [" + trkTypes.get(n).getName() + "]");
				List<TrksegType> trksegTypes = trkTypes.get(n).getTrkseg();
				logger.debug("         #" + n + " List of " + trksegTypes.size() + " <trkseg...");
				addGpxRecords("<trk>");
				addGpxRecords("<name>" + trkTypes.get(n).getName().replace("&", "&amp;") + "</name>", INDENT_PLUS);
				for (int m = 0; m < trksegTypes.size(); m++) {
					if (m > 0) {
						logger.debug("");
					}
					List<WptType> tkpts = trksegTypes.get(m).getTrkpt();
					logger.debug("            #" + m + " List of " + tkpts.size() + " <trkpt...");
					addGpxRecords("<trkseg>");
					for (int p = 0; p < tkpts.size(); p++) {
						if (p > 0) {
							logger.debug("");
						}
						logger.debug("               #" + p + " time [" + tkpts.get(p).getTime() + "]");
						logger.debug("               #" + p + " lat  [" + tkpts.get(p).getLat() +
							"] [" + new Formater().bigDecimalToString(tkpts.get(p).getLat(), scaleLatLonBigDecimal) + "]");
						logger.debug("               #" + p + " lon  [" + tkpts.get(p).getLon() +
							"] [" + new Formater().bigDecimalToString(tkpts.get(p).getLon(), scaleLatLonBigDecimal) + "]");
						logger.debug("               #" + p + " ele  [" + tkpts.get(p).getEle() +
							"] [" + new Formater().bigDecimalToString(tkpts.get(p).getEle(), scaleEleBigDecimal) + "]");
						addGpxRecords("<trkpt lat=\"" +
								new Formater().bigDecimalToString(tkpts.get(p).getLat(), scaleLatLonBigDecimal) +
								"\" lon=\"" + new Formater().bigDecimalToString(tkpts.get(p).getLon(), scaleLatLonBigDecimal) +
								"\">", (p == 0) ? INDENT_PLUS : INDENT_NONE);

						// Utilisation de 'Cmt' pour passer la ligne de commentaire ;-)
						String comment = tkpts.get(p).getCmt();
						if (comment != null) {
							logger.debug("               #" + p + " [" + comment + "]");
							addGpxRecords("<!-- " + comment + " -->", INDENT_PLUS);
							addGpxRecords("<ele>" + new Formater().bigDecimalToString(tkpts.get(p).getEle(), scaleEleBigDecimal) + "</ele>");
						}
						else {
							addGpxRecords("<ele>" + new Formater().bigDecimalToString(tkpts.get(p).getEle(), scaleEleBigDecimal) + "</ele>", INDENT_PLUS);
						}

						addGpxRecords("<time>" + tkpts.get(p).getTime() + "</time>");
						addGpxRecords("</trkpt>", INDENT_MOINS);
					}
					addGpxRecords("</trkseg>", INDENT_MOINS);
				}
				addGpxRecords("</trk>", INDENT_MOINS);
			}
			logger.debug("=> End of elements: </gpx>");
			addGpxRecords("</gpx>", INDENT_MOINS);

			logger.debug("");
			logger.debug("=> List of " + gpxRecords.size() + " records to write into [" + gpxFilePathName + "]");
			for (int n = 0; n < gpxRecords.size(); n++) {
				logger.debug("   #" + to05d(n) + " [" + gpxRecords.get(n) + "]");
			}
			logger.info("");
			logger.info("=> Writing into [" + gpxFilePathName + "]...");
			int nbrRecords = writeFile(gpxFilePathName, gpxRecords);
			logger.info("=> " + nbrRecords + " records written");

		} catch (FileNotFoundException ex) {
			logger.error("FileNotFoundException [" + ex.getMessage() + "]");
			exception = ex;
		} catch (DatatypeConfigurationException ex) {
			logger.error("DatatypeConfigurationException [" + ex.getMessage() + "]");
			exception = ex;
		} catch (JAXBException ex) {
			logger.info("JAXBException [" + ex.getMessage() + "]");
			exception = ex;
		} catch (IOException ex) {
			logger.error("IOException [" + ex.getMessage() + "]");
			exception = ex;
		} catch (Exception ex) {
			logger.error("Exception [" + ex.getMessage() + "]");
			exception = ex;
		}

		if (exception != null) {
			StackTraceElement[] stacks = exception.getStackTrace();
			for (StackTraceElement message: stacks) {
				logger.error(message.toString());
			}
		}

		logger.info("");
		logger.info("End of '" + Main.class.getName() + "'");

		System.exit(0);
	}
}
