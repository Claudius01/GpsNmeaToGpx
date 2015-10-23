package Gpx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import Misc.Formater;

public class Record {

	/* Analyzing and extraction in the record:
		
		[*3075AA063032.000t258B1AC94847.0701D1NEA00157.5163F1EG40.19s30.3z40.00H6116.55I6150112J204K5117.8L1Ma216b221c230d218e0f0g0h0i0j0k0l0m11n211]

		# 0: Received cksum [0x75]					(*) Checksum [*3X..]
		# 1: Type [A] Len [10] Value [063032.000]	(*) GPS time HHMMSS.000
		# 2: Type [t] Len [ 2] Value [58]			    PIC seconds
		# 3: Type [B] Len [ 1] Value [A]			(*) Valid datas
		# 4: Type [C] Len [ 9] Value [4847.0701]	(*) Lattitude (DDMM.mmmm - minutes decimales)
		# 5: Type [D] Len [ 1] Value [N]			(*) Hemisphere (N ou S)
		# 6: Type [E] Len [10] Value [00157.5163]	(*) Longitude (DDMM.mmmm - minutes decimales)
		# 7: Type [F] Len [ 1] Value [E]			(*) Position (E ou W)
		# 8: Type [G] Len [ 4] Value [0.19]			    Vitesse en noeuds
		# 9: Type [s] Len [ 3] Value [0.3]			    Vitesse en km/h  (UUU.D ou ---.-)
		#10: Type [z] Len [ 4] Value [0.00]			(*) Distance parcourue en km (UUU.DD ou UU.DD ou --.--)
		#11: Type [H] Len [ 6] Value [116.55]		    Cap
		#12: Type [I] Len [ 6] Value [150112]		(*) Date DDMMYY
		#13: Type [J] Len [ 2] Value [04]			    Nombre de satellites
		#14: Type [K] Len [ 5] Value [117.8]		(*) Altitude en metres (UUUU.D ou ----.)
		#15: Type [L] Len [ 1] Value [M]			(*) Unite [M]etre
		#16: Type [a] Len [ 2] Value [16]			    Id du 1st satellite
		#17: Type [b] Len [ 2] Value [21]				Id du 2nd satellite
		#18: Type [c] Len [ 2] Value [30]				...
		#19: Type [d] Len [ 2] Value [18]
		#20: Type [e] Len [ 0] Value []
		#21: Type [f] Len [ 0] Value []
		#22: Type [g] Len [ 0] Value []
		#23: Type [h] Len [ 0] Value []
		#24: Type [i] Len [ 0] Value []
		#25: Type [j] Len [ 0] Value []
		#26: Type [k] Len [ 0] Value []					Id du 11th satellite
		#27: Type [l] Len [ 0] Value []					Id du 12th satellite
		#28: Type [m] Len [ 1] Value [1]			    ???
		#29: Type [n] Len [ 2] Value [11]			    ???

		(*) Informations TLV decodees et verifiees pour contruire le .gpx
		(--.--) indique une information non significative (correlee au champ B1V - Invalid datas au lieu de B1A)

		Remarques:
			- Les caracteres du record sont valides et dans la plage [' ', ..., '~'] (cf. TestGpx.analyzeInputFile())
			- Suivant le JDK, "<String>.isEmpty() == false" est a remplacer par "<String>.length() != 0"
	*/

	private static Logger logger = Logger.getLogger(Record.class);

	// Constants
	static final Character CHAR_START = '*';		// 1st caractere du record
	static private final char
		TYPE_STATUS_CHECK = '*',
		TYPE_TIME         = 'A',
		TYPE_DATA_STATUS  = 'B',
		TYPE_LAT_VALUE    = 'C',
		TYPE_LAT_HEMI     = 'D',
		TYPE_LON_VALUE    = 'E',
		TYPE_LON_HEMI     = 'F',
		TYPE_CAP          = 'H',
		TYPE_DATE         = 'I',
		TYPE_ELE_VALUE    = 'K',
		TYPE_ELE_UNIT     = 'L',
		TYPE_SPEED_KMH    = 's',
		TYPE_DISTANCE     = 'z';

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

	private static int nbrRecords = 0;
	private static int nbrRecordsOk = 0;
	private static int nbrRecordsKo = 0;
	private static int nbrRecordsBadCks = 0;
	private static int nbrRecordsFiltered = 0;		// Nbr de records ne satisfaisant pas le filtrage (date, deplacement, etc.)

	private static Record recordPrevious = null;	// Record precedent pour lequel ...
	private static char checkumExpected = 0;		// ... la checksum 'checkumExpected' est attendue
	private static Record recordCurrent = null;		// Record courrant (le dernier sera ignore)

	private static List<Record> listRecordsToIgnore = new ArrayList<Record>();	// List des records non conserves
	private static List<Record> listRecordsToFilter = new ArrayList<Record>();	// List des records a conserver avant filtrage
	private static List<Record> listRecordsInvalid = new ArrayList<Record>();	// List des records invalides

	private static String sDatePrevious = "";
	static private String sLastRecord = "";			// Last record (for trace error)

	private static XMLGregorianCalendar xDateAndTimePrevious = null;	// <time>

	private int recordId = 0;						// Id du record [0...( 2^31 - 1)]
	private int recordIdAfterFitering = 0;			// Id du record [0...( 2^31 - 1)]
	private String sRecord = "";					// Current record
	private String sCheckum = "";					// sXyz: Champs extraits tel quel du record
	private String sTime = "";
	private String sDataStatus = "";
	private String sLattitude = "";
	private String sLongitude = "";
	private String sDate = "";
	private String sElevation = "";
	private String sSpeedKmh = "";
	private String sDistance = "";
	private String sCap = "";

	// Attributs pour la generation du .gpx (format XML)
	private XMLGregorianCalendar xDateAndTime = null;	// <time>
	private BigDecimal xLat = null;						// <lat>
	private BigDecimal xLon = null;						// <lon>
	private BigDecimal xEle = null;						// <ele>

	private double speedKmh = 0.0;				// Vitesse enregistree en km/h
	private double distEnrg = 0.0;				// Distance enregistree en km
	private double cap = 0.0;					// Cap en degres

	private double dist2DTotal = 0.0;			// Distance horizontale totale en km
	private double dist3DTotal = 0.0;			// Distance horizontale + verticale totale en km
	private static double denivelePos = 0.0;	// Denivele positif
	private static double deniveleNeg = 0.0;	// Denivele negatif

	private String sValue = "";
	private int checksumReceived = 0;

	private boolean flgValidRecord = false;			// A priori record invalide

	private boolean flgNewTrkt = false;				// Indication d'un nouveau 'trkt' a commencer
	private String trktName = "";					// Noms du 'trkt' d'appartenance
	private String waypointInfos = null;			// Informations du Waypoint "pose" ou null

	public Record() { }								// Pour recuperer les attributs globaux

	public Record(String value) throws Exception {

		logger.debug("Record([" + value + "])");

		sLastRecord = value;
		sRecord = value;

		// Test du 1st caractere
		if (sRecord.charAt(0) != TYPE_STATUS_CHECK) {
			logger.error("Invalid 1st char [" + sRecord.charAt(0) + "] != [" + TYPE_STATUS_CHECK + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de la checkum du precedent record a comparer avec 'sCheckumExpected'
		else if (getTlv(TYPE_STATUS_CHECK) == false) {
			logger.error("Invalid TLV 'TYPE_CHECKSUM' [" + TYPE_STATUS_CHECK + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'Data status'
		else if (getTlv(TYPE_DATA_STATUS) == false) {
			logger.error("Invalid TLV 'TYPE_DATA_STATUS' [" + TYPE_DATA_STATUS + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'time' (a faire avant la date ;-)
		else if (getTlv(TYPE_TIME) == false) {
			logger.error("Invalid TLV 'TYPE_TIME' [" + TYPE_TIME + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'date'
		else if (getTlv(TYPE_DATE) == false) {
			logger.error("Invalid TLV 'TYPE_DATE' [" + TYPE_DATE + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'lat' value (a faire avant 'hemi' ;-)
		else if (getTlv(TYPE_LAT_VALUE) == false) {
			logger.error("Invalid TLV 'TYPE_LAT_VALUE' [" + TYPE_LAT_VALUE + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'lat' hemi
		else if (getTlv(TYPE_LAT_HEMI) == false) {
			logger.error("Invalid TLV 'TYPE_LAT_HEMI' [" + TYPE_LAT_HEMI + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'lon' value (a faire avant 'hemi' ;-)
		else if (getTlv(TYPE_LON_VALUE) == false) {
			logger.error("Invalid TLV 'TYPE_LON_VALUE' [" + TYPE_LON_VALUE + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'lon' hemi
		else if (getTlv(TYPE_LON_HEMI) == false) {
			logger.error("Invalid TLV 'TYPE_LON_HEMI' [" + TYPE_LON_HEMI + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'ele'
		else if (getTlv(TYPE_ELE_VALUE) == false) {
			logger.error("Invalid TLV 'TYPE_ELE_VALUE' [" + TYPE_ELE_VALUE + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'ele' unite
		else if (getTlv(TYPE_ELE_UNIT) == false) {
			//logger.error("Invalid TLV 'TYPE_ELE_UNIT' [" + TYPE_ELE_UNIT + "]");
			//logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'speed'
		else if (getTlv(TYPE_SPEED_KMH) == false) {
			logger.error("Invalid TLV 'TYPE_SPEED_KMH' [" + TYPE_SPEED_KMH + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'distance'
		else if (getTlv(TYPE_DISTANCE) == false) {
			logger.error("Invalid TLV 'TYPE_DISTANCE' [" + TYPE_DISTANCE + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Recuperation de 'distance'
		else if (getTlv(TYPE_CAP) == false) {
			logger.error("Invalid TLV 'TYPE_CAP' [" + TYPE_CAP + "]");
			logger.error("in  [" + sRecord + "]");
			nbrRecordsKo++;
		}

		// Fin des recuperations
		else {
			nbrRecordsOk++;
			flgValidRecord = true;
		}

		// Test et calcul de la checksum
		testAndSetCksExpected();

		recordId = nbrRecords;
		recordPrevious = this;
		recordCurrent = this;

		nbrRecords++;
	}

	public int getRecordId() { return recordId; }
	public String getSRecord() { return sRecord; }
	public String getSLastRecord() { return sLastRecord; }
	public String getSDate() { return sDate; }
	public int getNbrRecords() { return nbrRecords; }
	public int getNbrRecordsOk() { return nbrRecordsOk; }
	public int getNbrRecordsKo() { return nbrRecordsKo; }
	public int getNbrRecordsBadCks() { return nbrRecordsBadCks; }
	public int getNbrRecordsFiltered() { return nbrRecordsFiltered; }
	public List<Record> getListRecordsToFilter() { return listRecordsToFilter; }
	public List<Record> getListRecordsToIgnore() { return listRecordsToIgnore; }
	public List<Record> getListRecordsInvalid() { return listRecordsInvalid; }
	public Record getRecordCurrent() { return recordCurrent; }

	public String getSCheckum() { return sCheckum; }

	// Remarque: - Avec une date comme '2012-01-15T07:52:59Z' dans le .gpx, la date correcte est 08:52:59
	//             => La publication avec 'YYYY-MM-DDTHH:MM:SSZ' est en heure GMT ;-)
	//           - TODO: Publier avec une date 'YYYY-MM-DDTHH:MM:SS.000+01:00' (+1 heure @ GMT - heure d'hiver a Paris)
	//           - La normalisation d'une date '2012-02-29T12:26:36.000+01:00' produit '2012-02-29T11:26:36.000Z'
	//             ce qui correspond a une date GMT attendue par le site de publication
	//             => 'YYYY-MM-DDTHH:MM:SS.000+HH:MM' est la date GMT +/- TZ => Heure locale en final ;-)
	public XMLGregorianCalendar getXDateAndTime() { return xDateAndTime/*.normalize()*/; }	// normalize => YYYY-MM-DDTHH:MM:SSZ
	public BigDecimal getXLat() { return xLat; }
	public BigDecimal getXLon() { return xLon; }
	public BigDecimal getXEle() { return xEle; }

	public long getTime() { return xDateAndTime.toGregorianCalendar().getTimeInMillis(); }	// Permet les comparaisons de date
	public double getSpeedKmh() { return speedKmh; }
	public double getDistEnrg() { return distEnrg; }
	public double getCap() { return cap; }

	public boolean isValidRecord() { return flgValidRecord; }
	public void setIdAfterFiltering(int value) { recordIdAfterFitering = value; }
	public int getIdAfterFiltering() { return recordIdAfterFitering; }
	public double getDist2DTotal() { return dist2DTotal; }
	public void setDist2DTotal(double value) { dist2DTotal = value; }
	public double getDist3DTotal() { return dist3DTotal; }
	public void setDist3DTotal(double value) { dist3DTotal = value; }
	public double getDenivelePos() { return denivelePos; }
	public void setDenivelePos(double value) { denivelePos = value; }
	public double getDeniveleNeg() { return deniveleNeg; }
	public void setDeniveleNeg(double value) { deniveleNeg = value; }

	private boolean getTlv(Character type) throws Exception {

		boolean flgRtn = false;
		switch (type) {
		case TYPE_STATUS_CHECK:
		case TYPE_TIME:
		case TYPE_DATA_STATUS:
		case TYPE_LAT_VALUE:
		case TYPE_LAT_HEMI:
		case TYPE_LON_VALUE:
		case TYPE_LON_HEMI:
		case TYPE_DATE:
		case TYPE_ELE_VALUE:
		case TYPE_ELE_UNIT:
		case TYPE_SPEED_KMH:
		case TYPE_DISTANCE:
		case TYPE_CAP:
			// Parcours du record, debut sur le 1st Tlv et saut jusqu'a trouver 'type'
			int offset = 0;		// Index sur 'T'
			int len = 0;
			boolean flgFound = false;
			do {
				String sType = sRecord.substring(offset, offset + 1);
				offset += 1;	// Index sur 'L'
				String sLen = sRecord.substring(offset, offset + 1);
				offset += 1;	// Index sur 'V'
				int base = 16;
				try {
					len = Integer.valueOf(sLen, base);
				}
				catch (NumberFormatException e) {
					logger.error("Invalid number [" + sLen + "] in base [" + base + "]");
					return false;
				}
				if (sType.equals(type.toString())) {
					flgFound = true;
					break;
				}
				offset += len;
			}
			while (offset < sRecord.length());
			if (flgFound == false) {
				//logger.error("Type [" + type + "] not found in [" + sRecord + "]");
				return false;
			}
			sValue = sRecord.substring(offset, offset + len);
			logger.debug("Type [" + type + "] Len [" + to2d(len) + "] Value [" + sValue + "]");
			flgRtn = testAndSetValues(type);
			sValue = "";
			break;
		default:
			throw new Exception("getTlv(): Type [" + type + "] not supported");
		}
		return flgRtn;
	}

	private boolean testAndSetValues(Character type) throws Exception {

		boolean flgRtn = true;
		switch (type) {
		case TYPE_STATUS_CHECK:
			sDataStatus = sValue.substring(1, 2);
			sValue = sValue.substring(1, 3);
			int base = 16;
			try {
				checksumReceived = Integer.valueOf(sValue, base);
			}
			catch (NumberFormatException e) {
				logger.error("Invalid checksum [0x" + sValue + "] in base [" + base + "]");
				return false;
			}
			sCheckum = sValue;
			logger.debug("Status [" + sDataStatus + "] Checksum [0x" +
				Integer.toHexString(0x100 + (checksumReceived & 0xff)).toString().substring(1) + "] received");
			break;
		case TYPE_TIME:
			sTime = sValue;
			//logger.info("sTime [" + sTime + "]");
			break;
		case TYPE_DATA_STATUS:
			if (sValue.equals("A")) {
				sDataStatus = sValue;
			}
			else {
				//logger.error("Data status [" + sValue + "] not OK");
				return false;				
			}
			break;
		case TYPE_LAT_VALUE:
			sLattitude = sValue;
			break;
		case TYPE_LAT_HEMI:
			if (sLattitude.length() != 0) {
				if (sValue.equals("N")) {
					xLat = convToDegDecimal(sLattitude);
				}
				else if (sValue.equals("S")) {
					xLon = convToDegDecimal("-" + sLattitude);
				}
				else {
					logger.error("Invalid hemisphere [" + sValue + "] ! [N | S]");
				}
			}
			else {			
				logger.error("sLattitude empty");
				return false;
			}
			break;
		case TYPE_LON_VALUE:
			sLongitude = sValue;
			break;
		case TYPE_LON_HEMI:
			if (sLongitude.length() != 0) {
				if (sValue.equals("E")) {
					xLon = convToDegDecimal(sLongitude);
				}
				else if (sValue.equals("W")) {
					xLon = convToDegDecimal("-" + sLongitude);
				}
				else {
					logger.error("Invalid hemisphere [" + sValue + "] ! [E | W]");
				}
			}
			else {
				logger.error("sLongitude empty");
				return false;
			}
			break;
		case TYPE_DATE:
			sDate = sValue;
			logger.debug("sDate [" + sDate + "]");
			if (sDate.length() != 0 && sTime.length() != 0) {
				// Conversion DDMMYY HHMMSS.000 => YYYY/MM/DD HH:MM:SS
				if (sDate.length() == "DDMMYY".length() && sDate.matches("[0-9][0-9][0-9][0-9][0-9][0-9]")) {
					String loc_dateAndTime = "20" + sDate.substring(4, 6) + "/" +	// Year
						sDate.substring(2, 4) +	"/" +								// Month
						sDate.substring(0, 2);										// Day

					// Trace de changement de date
					boolean flgNewSplit = false;
					if (loc_dateAndTime.equals(sDatePrevious) == false) {
						sDatePrevious = loc_dateAndTime;
						flgNewSplit = true;
					}

					if (sTime.length() == "HHMMSS.000".length() && sTime.matches("[0-9][0-9][0-9][0-9][0-9][0-9][.][0-9][0-9][0-9]")) {
						loc_dateAndTime += " " +
							sTime.substring(0, 2) +	":" +			// Hours
							sTime.substring(2, 4) +	":" +			// Minutes
							sTime.substring(4, 6);					// Seconds

						xDateAndTime = new Formater().dateToXmlCalendar(loc_dateAndTime);

						// Test "Back to the future"
						if (xDateAndTimePrevious == null) {
							xDateAndTimePrevious = xDateAndTime;
						}
						else if (xDateAndTime.compare(xDateAndTimePrevious) == DatatypeConstants.LESSER) {
							logger.error("Back to the future with [" + sRecord + "]");
							return false;
						}
						xDateAndTimePrevious = xDateAndTime;

						if (flgNewSplit == true) {
							logger.warn("Change date to [" + loc_dateAndTime + "] with [" + sRecord + "]");
						}
					}
					else {				
						logger.error("Invalid Time format [" + sTime + "]");
						return false;
					}
				}
				else {				
					logger.error("Invalid Date format [" + sDate + "]");
					return false;
				}
			}
			else {
				logger.error("Date or Time field empty");				
			}
			break;
		case TYPE_ELE_VALUE:
			sElevation = sValue;
			try {
				Double.parseDouble(sElevation);
			}
			catch (NumberFormatException e) {
				logger.error("Wrong elevation value [" + sElevation + "]");
				return false;				
			}
			break;
		case TYPE_ELE_UNIT:
			if (sValue.equals("M") && sElevation.length() != 0) {
				xEle = stringToBigDecimal(sElevation);
			}
			else {
				//logger.error("sElevation empty or wrong unit [" + sValue + "] != [M]");
				return false;
			}
			break;
		case TYPE_SPEED_KMH:
			if (sValue.equals("---.-") == false) {
				sSpeedKmh = sValue;
				try {
					speedKmh = Double.parseDouble(sSpeedKmh);
				}
				catch (NumberFormatException e) {
					logger.error("Wrong speed value [" + sSpeedKmh + "]");
					return false;				
				}
			}
			break;
		case TYPE_DISTANCE:
			if (sValue.equals("--.--") == false) {
				sDistance = sValue;
				try {
					distEnrg = Double.parseDouble(sDistance);
				}
				catch (NumberFormatException e) {
					logger.error("Wrong speed value [" + sDistance + "]");
					return false;				
				}
			}
			break;
		case TYPE_CAP:
			if (sValue.length() != 0) {
				sCap = sValue;
				try {
					cap = Double.parseDouble(sCap);
				}
				catch (NumberFormatException e) {
					logger.error("Wrong speed value [" + sCap + "]");
					return false;				
				}
			}
			break;
		default:
			throw new Exception("testAndSetValues(): Type [" + type + "] not supported");
		}
		return flgRtn;
	}

	private void testAndSetCksExpected() {

		// Test de la checksum attendue
		if (nbrRecords > 0) {
			if (checksumReceived == checkumExpected) {
				updateListRecords(this.recordPrevious);
				logger.debug("=> Record previous to filter");
			}
			else {
				nbrRecordsBadCks++;
				flgValidRecord = false;
				// Ne sachant pas discriminer l'enregistrement invalide,
				// les 2 records precedent et en cours seront ignores
				updateListRecordsToIgnore(this.recordPrevious);
				updateListRecordsToIgnore(this);
				logger.debug("=> 2 Records (the previous and it) to suppress (invalid checksum received @ expected [0x" +
						Integer.toString(checksumReceived, 16) + "] != [0x" +
						Integer.toString(checkumExpected, 16) + "])");
			}
		}
		else {
			updateListRecordsToIgnore(this);
			logger.debug("=> Record to ignore (no checksum expected available)");
		}

		// Calcul de la checksum sans le champ TLV TYPE_STATUS_CHECK (*3SCC...)
		if (sRecord.length() >= "*3SCC".length()) {
			checkumExpected = 0;
			for (int n = "*3SCC".length(); n < sRecord.length(); n++) {
				checkumExpected ^= sRecord.charAt(n);
			}
			logger.debug("Checkum expected  [0x" + Integer.toString(checkumExpected, 16) + "]");
		}
		else {
			logger.error("Record too short (" + sRecord.length() + " bytes)");
		}
	}

	private void updateListRecords(Record record) {

		if (listRecordsToFilter.contains(record) == false) {
			// Ne rien faire si deja ignore
			if (listRecordsToIgnore.contains(record) == false) {
				// Conservation du record si valide
				if (record.isValidRecord()) {
					listRecordsToFilter.add(record);
				}
				else if (listRecordsInvalid.contains(record) == false) {
					listRecordsInvalid.add(record);
				}
			}
		}
		else {
			logger.warn("Record #" + to05d(recordId) + " already in the list of records to filter");
		}
	}

	private void updateListRecordsToIgnore(Record record) {

		// A ignorer si pas deja le cas, si pas conserve et si pas invalide
		if (listRecordsToIgnore.contains(record) == false
		 && listRecordsToFilter.contains(record) == false
		 && listRecordsInvalid.contains(record) == false) {
			listRecordsToIgnore.add(record);
		}
	}

	// Conversion format ...DDDMM.mmmm en DD.dddd... (degres decimaux)
	private BigDecimal convToDegDecimal(String iValue) throws Exception {

		// Suppression et memorisation de l'eventuel signe
		String value = iValue;
		boolean flgNegatif = false;
		if (iValue.startsWith("-")) {
			flgNegatif = true;
			value = iValue.substring(1);
		}
		try {
			String unit = value.split("[.]")[0];
			String dec = value.split("[.]")[1];
			String sDegrees = unit.substring(0, unit.length() - 2);
			String sMinutes = unit.substring(unit.length() - 2, unit.length());
			double minutes = Double.parseDouble(sMinutes + "." + dec);
			double degrees = Double.parseDouble(sDegrees) + (minutes / 60.0);
			if (flgNegatif) {
				degrees = -degrees;
			}
			return new BigDecimal(degrees);
		}
		catch(PatternSyntaxException  e) {
			logger.error("convToDegDecimal(" + value + "): PatternSyntaxException");
			throw e;
		}
		catch(NumberFormatException e) {
			logger.error("convToDegDecimal(" + value + "): NumberFormatException");
			throw e;
		}
	}

	// Conversion UUU.DDD... en un BigDecimal
	private BigDecimal stringToBigDecimal(String value) throws Exception {

		try {
			BigDecimal bigDecValue = new BigDecimal(value);

			return bigDecValue;
		}
		catch (NumberFormatException  e) {
			throw new NumberFormatException(e.getMessage());
		}
		catch (Exception  e) {
			throw new Exception(e.getMessage());
		}
	}

	public void setFlgNewTrkt(boolean value) { flgNewTrkt = value; }
	public boolean getFlgNewTrkt() { return  flgNewTrkt; }

	public void setTrktName(String value) { trktName = value; }
	public String getTrktName() { return  trktName; }

	public String getWaypointInfos() { return waypointInfos; }
	public void setWaypointInfos(String value) { waypointInfos = value; }

}
