package Gpx;

import org.apache.log4j.Logger;
import org.xml.sax.*;

import com.sun.media.sound.MidiOutDeviceProvider;

import Misc.Formater;

import javax.xml.xpath.*; 
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.*; 
import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

public class Configuration {

	private static Logger logger = Logger.getLogger(Configuration.class);

	private static String configPath = "";
	private static String configFile = "";

	private static File configPathFile = null;

	private String traceName = "";
	private String product = "";
	private String version = "";
	private String author = "";
	private String description = "";
	private String trackpointName = "";
	private String dateTime = "";

	private String directory = "";				// Repertoire de travail
	private String inputFileName = "";			// Fichier d'entree de la cle USB (1! record)
	private String startDate = "";				// Date de debut au format YYYYMMDD HHMMSS (codage PIC)
	private String endDate = null;				// Date de fin au format YYYYMMDD HHMMSS (codage PIC)
	private String duration = null;				// Duree => Date de fin en l'absence de celle-ci
	private String outputFiles = "";			// Base des fichiers de sortie pour une plage horaire (n records valides)
	private int scaleLatLonBigDecimal = 0;		// Nbr de chiffres apres la virgule (troncature)
	private int scaleEleBigDecimal = 0;			// Nbr de chiffres apres la virgule (troncature)
	private int nbrIndentation = 0;				// Nbr d'espaces pour l'indentation (1 espace au moins)

	private int durationBetweenPeriods = 0;		// Nbr de secondes entre periodes "Start" => "End" date

	private double speedThresholdHigh = 0.0;	// Seuil haut vitesse en km/h pour une meme periode
	private double speedThresholdLow = 0.0;		// Seuil bas vitesse en km/h pour une meme periode
	private int speedTimeoutHigh = 0;			// Timeout passage au dessus de 'speedThresholdHigh'
	private int speedTimeoutLow = 0;			// Timeout passage en deca de 'speedThresholdLow'

	private int distBetweenSamples = 0;			// Nbr de metres entre echantillons de mesures => .gpx

	private boolean flgSplit = false;			// Decoupage du fichier 'InputFileName' en n fichier(s) 'OutputFiles.keyn'
	private boolean flgOutputXml = false;		// Generation du .xml avant .gpx (true/false = oui/non = .xml/stream) 
	private boolean flgOutputRecords = false;	// Generation du .rcd
	private boolean flgInverse = false;			// Inversion de la trace

	private List<WayPoint> cutDates = new ArrayList<WayPoint>();
	private List<WayPoint> smoothDefinitions = new ArrayList<WayPoint>();
	private List<WayPoint> waypoints = new ArrayList<WayPoint>();

	private String noneToZero(String value) { return (value.compareToIgnoreCase("none") == 0 ? "0" : value); }


	/** Accesseur : Obtention de la valeur d'un attribut d'un fichier xml
	 * @param fichier Descripteur de fichier 
	 * @param expression Xpath pour la recherche 
	 * @param retour Type QName en retour 
	 */
	
	private String evalSAX(File fichier, String expression, QName retour) throws Exception {
		
		logger.debug("evalSAX([" + fichier.getAbsolutePath() + "], [" + expression + "], ...)");
		String value = null;
		try {
			// creation de la source
			InputSource source = new InputSource(new FileInputStream(fichier));
			
			// creation du XPath 
			XPathFactory fabrique = XPathFactory.newInstance();
			XPath xpath = fabrique.newXPath();
			
			// evaluation de l'expression XPath
			XPathExpression exp = xpath.compile(expression);
			//Object resultat = exp.evaluate(source, retour);

			String sRtn = exp.evaluate(source, retour).toString();
			logger.debug("=> return [" + sRtn + "]");
			return sRtn;

		} catch(XPathExpressionException xpee){
			xpee.printStackTrace();
		} catch(IOException ioe){
			if (logger.isDebugEnabled() == true) {
				ioe.printStackTrace();
			}
			throw ioe;
		}

		return value;

	}

	/** Accesseur : Obtention d'une valeur dont l'expression Xpath est fournie
	 * @param exp_xpath Expression Xpath 
	 * @return valeur trouvee
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterValue(String exp_xpath) throws Exception {

		logger.debug("getParameterValue(" + exp_xpath + ")");
		try {
			return evalSAX(configPathFile, exp_xpath, XPathConstants.STRING);
		} catch (Exception e){
			throw e;	
		}
	}

	/** Accesseur : Obtention du repertoire de travail
	 * @param  None
	 * @return Nom du repertoire configure
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterDirectory() throws Exception {

		try {
			String expression = "//Configuration/Behavior/Directory";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Directory' parameter");	
		}
	}

	/** Accesseur : Obtention du nombre de chiffres apres la virgule pour lat/lon
	 * @param  None
	 * @return Nombre de chiffres configures
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterScaleLatLonBigDecimal() throws Exception {

		try {
			String expression = "//Configuration/Behavior/ScaleLatLonBigDecimal";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			if (value >= 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'ScaleLatLonBigDecimal' parameter");	
		}
	}

	/** Accesseur : Obtention du nombre de chiffres apres la virgule pour ele
	 * @param  None
	 * @return Nombre de chiffres configures
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterScaleEleBigDecimal() throws Exception {

		try {
			String expression = "//Configuration/Behavior/ScaleEleBigDecimal";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			if (value >= 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'ScaleEleBigDecimal' parameter");	
		}
	}

	/** Accesseur : Obtention du nombre de characteres d'indentation
	 * @param  None
	 * @return Nombre de characteres d'indentation configures
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterNbrIndentation() throws Exception {

		try {
			String expression = "//Configuration/Behavior/NbrIndentation";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			if (value > 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'NbrIndentation' parameter");	
		}
	}
	
	/** Accesseur : Obtention du nombre de secondes entre 2 periodes differentes
	 * @param  None
	 * @return Nombre de secondes entre 2 periodes differentes
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterDurationBetweenPeriods() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Duration/BetweenPeriods";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			if (value > 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'BetweenPeriods' parameter");	
		}
	}

	/** Accesseur : Obtention du seuil haut de la vitesse
	 * @param  None
	 * @return Nombre du seuil haut de la vitesse
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSpeedThresholdHigh() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Speed/ThresholdHigh";
			Double value = Double.parseDouble(evalSAX(configPathFile, expression, XPathConstants.STRING));
			return value;
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'ThresholdHigh' parameter");	
		}
	}

	/** Accesseur : Obtention du seuil bas de la vitesse
	 * @param  None
	 * @return Nombre du seuil bas de la vitesse
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSpeedThresholdLow() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Speed/ThresholdLow";
			Double value = Double.parseDouble(evalSAX(configPathFile, expression, XPathConstants.STRING));
			return value;
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'ThresholdLow' parameter");	
		}
	}

	/** Accesseur : Obtention du timeout depassement seuil haut
	 * @param  None
	 * @return Nombre du timeout depassement seuil haut
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterSpeedTimeoutHigh() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Speed/TimeoutHigh";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			return value;
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'TimeoutHigh' parameter");	
		}
	}

	/** Accesseur : Obtention du timeout depassement seuil bas
	 * @param  None
	 * @return Nombre du timeout depassement seuil bas
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterSpeedTimeoutLow() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Speed/TimeoutLow";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			return value;
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'TimeoutLow' parameter");	
		}
	}

	/** Accesseur : Obtention du nombre de metres entre 2 echantillons => .gpx
	 * @param  None
	 * @return Nombre de metres entre 2 echantillons => .gpx
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterDistBetweenSamples() throws Exception {

		try {
			String expression = "//Configuration/Filtering/Distance/BetweenSamples";
			Integer value = Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));
			if (value > 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'BetweenSamples' parameter");	
		}
	}

	/** Accesseur : Obtention du nom de l'auteur
	 * @param  Nom de la trace
	 * @return Nom de l'auteur
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterAuthor() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Author";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Author' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention du nom de l'auteur
	 * @param  Nom de la trace
	 * @return Nom de l'auteur
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterDescription() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Description";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Description' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention de la date de creation / publication
	 * @param  Nom de la trace
	 * @return Date de creation / publication
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterDateTime(boolean flgAddTZ) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/DateTime";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value.trim();
			}
			else {
				return new Formater().getToday(flgAddTZ);
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'DateTime' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention du nom de l'auteur
	 * @param  Nom de la trace
	 * @return Nom de l'auteur
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterTrackpointName() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/TrackpointName";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'TrackpointName' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention du nom du fichier d'entree
	 * @param  Nom de la trace
	 * @return Nom du fichier d'entree configure
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterInputFileName() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/InputFileName";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'InputFileName' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention du nom du fichier d'entree
	 * @param  Nom de la trace
	 * @return Nom du fichier de sortie configure
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterOutputFiles() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/OutputFiles";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'OutputFile' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention de la date et heure de debut
	 * @param  Nom de la trace
	 * @return Date et heure de debut
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterStartDate() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/StartDate";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= ("YYYY/MM/DD HH:MM:SS".length())) {
				return value.trim();
			}
			else {
				throw new Exception();
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'StartDate' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention de la date et heure de fin
	 * @param  Nom de la trace
	 * @return Date et heure de fin
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterEndDate() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/EndDate";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= ("YYYY/MM/DD HH:MM:SS".length())) {
				return value.trim();
			}
			else {
				return "";
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'EndDate' parameter for trace [" + traceName + "]");
		}
	}

	/** Accesseur : Obtention de la duree depuis la date de debut 
	 * @param  Nom de la trace
	 * @return Duree depuis la date de debut
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterDuration() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Duration";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= ("H:MM".length()) && value.contains(":") == true) {
				return value;
			}
			else {
				return "";
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Duration' parameter for trace " + traceName);
		}
	}

	/** Accesseur : Obtention du flag de decoupage du fichier d'entree
	 * @param  Nom de la trace
	 * @return Flag de decoupage
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private boolean getParameterFlgSplit() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/FlgSplit";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.equalsIgnoreCase("YES") == true) {
				return true;
			}
			else {
				return false;
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'FlgSplit' parameter for trace " + traceName);
		}
	}

	/** Accesseur : Obtention du flag generation du .xml
	 * @param  Nom de la trace
	 * @return Flag generation du .xml
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private boolean getParameterFlgOutputXml() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/FlgOutputXml";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.equalsIgnoreCase("YES") == true) {
				return true;
			}
			else {
				return false;
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'FlgOutputRecords' parameter for trace " + traceName);
		}
	}

	/** Accesseur : Obtention du flag generation du .rcd
	 * @param  Nom de la trace
	 * @return Flag generation du .rcd
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private boolean getParameterFlgOutputRecords() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/FlgOutputRecords";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.equalsIgnoreCase("YES") == true) {
				return true;
			}
			else {
				return false;
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'FlgOutputRecords' parameter for trace " + traceName);
		}
	}

	/** Accesseur : Obtention du flag d'inversion de la trace
	 * @param  Nom de la trace
	 * @return Flag d'inversion
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private boolean getParameterFlgInverse() throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/FlgInverse";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.equalsIgnoreCase("YES") == true) {
				return true;
			}
			else {
				return false;
			}
		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'FlgInverse' parameter for trace " + traceName);
		}
	}

	/** Accesseur : Obtention du nombre de plages horaires lissees
	 * @param  Nom de la trace
	 * @return Nombre de plages pour ensuite les recuperer une par une
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterNbrSmooth() throws Exception {

		try {
			String expression = "count(//Configuration/Trace[@name='" + traceName + "']/Smooth)";

			return Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth' parameter for trace " + traceName);
		}

	} // getParameterNbrSmooth()

	/** Accesseur : Obtention de l'heure de debut de la nieme plage horaire lissee
	 * @param  Nom de la trace
	 * @return Heure de debut
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterSmoothStartTime(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/StartTime";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return value.trim();
			}
			else {
				return null;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/StartTime' parameter for trace " + traceName);
		}

	} // getParameterSmoothStartTime()

	/** Accesseur : Obtention de l'heure de fin de la nieme plage horaire lissee
	 * @param  Nom de la trace
	 * @return Heure de fin
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterSmoothEndTime(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/EndTime";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return value.trim();
			}
			else {
				return null;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/EndTime' parameter for trace " + traceName);
		}

	} // getParameterSmoothEndTime()

	/** Accesseur : Obtention de la lattitude du coin en haut a gauche du nieme Smooth
	 * @param  Nom de la trace
	 * @return Lattitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothTopLeftCornerLat(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/TopLeftCorner/Lat";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/TopLeftCorner/Lat' parameter for trace " + traceName);
		}

	} // getParameterSmoothTopLeftCornerLat()

	/** Accesseur : Obtention de la longitude du coin en haut a gauche du nieme Smooth
	 * @param  Nom de la trace
	 * @return Longitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothTopLeftCornerLon(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/TopLeftCorner/Lon";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/TopLeftCorner/Lon' parameter for trace " + traceName);
		}

	} // getParameterSmoothTopLeftCornerLon()
	
	/** Accesseur : Obtention de la lattitude du coin en bas a droite du nieme Smooth 
	 * @param  Nom de la trace
	 * @return Lattitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothBottomRightCornerLat(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/BottomRightCorner/Lat";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/BottomRightCorner/Lat' parameter for trace " + traceName);
		}

	} // getParameterSmoothBottomRightCornerLat()

	/** Accesseur : Obtention de la longitude du coin en bas a droite du nieme Smooth
	 * @param  Nom de la trace
	 * @return Longitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothBottomRightCornerLon(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/BottomRightCorner/Lon";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/BottomRightCorner/Lon' parameter for trace " + traceName);
		}

	} // getParameterSmoothBottomRightCornerLon()
	
	/** Accesseur : Obtention de la precision pour Smooth From/to
	 * @param  Nom de la trace
	 * @return Precision
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothFromToPrecision(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/Precision";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null) {
				return 0.00001 * Double.parseDouble(value.trim());
			}
			else {
				return 0.00005;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/Precision' parameter for trace " + traceName);
		}

	} // getParameterSmoothFromToPrecision()

	/** Accesseur : Obtention de la lattitude du From du nieme Smooth
	 * @param  Nom de la trace
	 * @return Lattitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothFromLat(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/From/Lat";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/From/Lat' parameter for trace " + traceName);
		}

	} // getParameterSmoothFromLat()

	/** Accesseur : Obtention de la longitude du From du nieme Smooth
	 * @param  Nom de la trace
	 * @return Longitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothFromLon(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/From/Lon";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/From/Lon' parameter for trace " + traceName);
		}

	} // getParameterSmoothFromLon()
	
	/** Accesseur : Obtention de la lattitude du To du nieme Smooth 
	 * @param  Nom de la trace
	 * @return Lattitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothToLat(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/To/Lat";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/To/Lat' parameter for trace " + traceName);
		}

	} // getParameterSmoothToLat()

	/** Accesseur : Obtention de la longitude du To du nieme Smooth
	 * @param  Nom de la trace
	 * @return Longitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterSmoothToLon(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Smooth["+(n+1)+"]/To/Lon";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Smooth[" + n + "]/To/Lon' parameter for trace " + traceName);
		}

	} // getParameterSmoothToLon()
	
	/** Accesseur : Obtention du nombre de plages horaires coupees
	 * @param  Nom de la trace
	 * @return Nombre de plages pour ensuite les recuperer une par une
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterNbrCut() throws Exception {

		try {
			String expression = "count(//Configuration/Trace[@name='" + traceName + "']/Cut)";

			return Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Cut' parameter for trace " + traceName);
		}

	} // getParameterNbrCut()

	/** Accesseur : Obtention de la description de la nieme plage horaire coupee
	 * @param  Nom de la trace
	 * @return Heure de la description
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterCutDescription(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Cut["+(n+1)+"]/Description";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Cut[" + n + "]/Description' parameter for trace " + traceName);
		}

	} // getParameterCutDescription()

	/** Accesseur : Obtention de l'heure de debut de la nieme plage horaire coupee
	 * @param  Nom de la trace
	 * @return Heure de debut
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterCutStartTime(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Cut["+(n+1)+"]/StartTime";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return value.trim();
			}
			else {
				throw new Exception();
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Cut[" + n + "]/StartTime' parameter for trace " + traceName);
		}

	} // getParameterCutStartTime()

	/** Accesseur : Obtention de l'heure de fin de la nieme plage horaire coupee
	 * @param  Nom de la trace
	 * @return Heure de fin
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterCutEndTime(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Cut["+(n+1)+"]/EndTime";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return value.trim();
			}
			else {
				throw new Exception();
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Cut[" + n + "]/EndTime' parameter for trace " + traceName);
		}

	} // getParameterCutEndTime()

	/** Accesseur : Obtention du nombre de waypoints
	 * @param  Nom de la trace
	 * @return Nombre de waypoints pour ensuite les recuperer une par une
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private int getParameterNbrWaypoints() throws Exception {

		try {
			String expression = "count(//Configuration/Trace[@name='" + traceName + "']/Waypoint)";

			return Integer.parseInt(evalSAX(configPathFile, expression, XPathConstants.STRING));

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint' parameter for trace " + traceName);
		}

	} // getParameterNbrWaypoints()

	/** Accesseur : Obtention du nom du nieme waypoint
	 * @param  Nom de la trace
	 * @return Nom du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterWaypointName(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Waypoint["+(n+1)+"]/Name";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint[" + n + "]/Name' parameter for trace " + traceName);
		}

	} // getParameterWaypointDescription()

	/** Accesseur : Obtention de la description du nieme waypoint
	 * @param  Nom de la trace
	 * @return Description du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterWaypointDescription(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Waypoint["+(n+1)+"]/Description";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() != 0) {
				return value;
			}
			else {
				throw new Exception();
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint[" + n + "]/Description' parameter for trace " + traceName);
		}

	} // getParameterWaypointDescription()

	/** Accesseur : Obtention de l'heure du nieme waypoint
	 * @param  Nom de la trace
	 * @return Heure du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private String getParameterWaypointTime(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Waypoint["+(n+1)+"]/Time";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return value.trim();
			}
			else {
				return null;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint[" + n + "]/Time' parameter for trace " + traceName);
		}

	} // getParameterWaypointTime()

	/** Accesseur : Obtention de la lattitude du nieme waypoint
	 * @param  Nom de la trace
	 * @return Lattitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterWaypointLat(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Waypoint["+(n+1)+"]/Lat";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint[" + n + "]/Lat' parameter for trace " + traceName);
		}

	} // getParameterWaypointLat()

	/** Accesseur : Obtention de la longitude du nieme waypoint
	 * @param  Nom de la trace
	 * @return Longitude du waypoint
	 * @throws Exception
	 * Generate Exception for a invalid treatment
	 */

	private double getParameterWaypointLon(int n) throws Exception {

		try {
			String expression = "//Configuration/Trace[@name='" + traceName + "']/Waypoint["+(n+1)+"]/Lon";
			String value = evalSAX(configPathFile, expression, XPathConstants.STRING);
			if (value != null && value.length() >= "HH:MM:SS".length()) {
				return Double.parseDouble(value.trim());
			}
			else {
				return Double.NaN;
			}

		} catch (Exception e){
			throw new Exception(Configuration.class.getName() + ": Not found or invalid 'Waypoint[" + n + "]/Lon' parameter for trace " + traceName);
		}

	} // getParameterWaypointLon()

	// Prise waypoints et des plages horaires
	// Test de validite de celles-ci (debut de l'analyse < start < end < fin de l'analyse)
	// Construction de 2 listes 'Cut' et 'Smooth' classees par ordre chronologique 
	public void getAndTestTimeRanges() throws Exception {

		try {
			LoggerExtend loggerExtend = new LoggerExtend();
			String yyyy_mm_dd = startDate.substring(0, "YYYY/MM/DD".length());

			int nbr = getParameterNbrCut();
			logger.debug("");
			logger.debug("List of the " + nbr + " time ranges cut");
			int n = 0;
			for (n = 0; n < nbr; n++) {
				String desc = getParameterCutDescription(n);
				String fromDate = yyyy_mm_dd + " " + getParameterCutStartTime(n);
				String toDate = yyyy_mm_dd + " " + getParameterCutEndTime(n);
				logger.debug("#" + n + " [" + desc + "] [" + fromDate + "] to [" + toDate + "]");
				if (new Formater().isValidDates(startDate, fromDate, toDate, endDate) == false) {
					logger.error("#" + n + " [" + desc + "] [" + fromDate + "] to [" + toDate + "]");
					throw new Exception(Configuration.class.getName() + ": Invalid dates");
				}
				else {
					new WayPoint().insert(cutDates, new WayPoint(null, desc, fromDate, toDate));
				}
			}
			loggerExtend.info("");
			loggerExtend.info("List of the " + nbr + " time ranges cut sorted");
			n = 0;
			for (WayPoint waypoint: cutDates) {
				loggerExtend.info("   #" + (n++) + " [" + waypoint.getDescription() +
					"] [" + waypoint.getDateFrom() + "] to [" + waypoint.getDateTo() + "]");				
			}

			nbr = getParameterNbrSmooth();
			logger.debug("");
			logger.debug("List of the " + nbr + " time ranges smoothed");
			for (n = 0; n < nbr; n++) {
				double smoothTopLeftCornerLat = getParameterSmoothTopLeftCornerLat(n);
				double smoothTopLeftCornerLon = getParameterSmoothTopLeftCornerLon(n);
				double smoothBottomRightCornerLat   = getParameterSmoothBottomRightCornerLat(n);
				double smoothBottomRightCornerLon   = getParameterSmoothBottomRightCornerLon(n);
				boolean flgSmoothCorner = (Double.isNaN(smoothTopLeftCornerLat) == true && Double.isNaN(smoothTopLeftCornerLon) == true);
				flgSmoothCorner &= (Double.isNaN(smoothBottomRightCornerLat) == true && Double.isNaN(smoothBottomRightCornerLon) == true);

				double smoothFromLat = getParameterSmoothFromLat(n);
				double smoothFromLon = getParameterSmoothFromLon(n);
				double smoothToLat   = getParameterSmoothToLat(n);
				double smoothToLon   = getParameterSmoothToLon(n);
				boolean flgSmoothFromTo = (Double.isNaN(smoothFromLat) == true && Double.isNaN(smoothFromLon) == true);
				flgSmoothFromTo &= (Double.isNaN(smoothToLat) == true && Double.isNaN(smoothToLon) == true);

				if (getParameterSmoothStartTime(n) != null && getParameterSmoothEndTime(n) != null) {
					if (flgSmoothCorner == false || flgSmoothFromTo == false) {
						throw new Exception(Configuration.class.getName() + ": Invalid Smooth Corner or From/To Lat/Lon with Start/End dates");					
					}
					String fromDate = yyyy_mm_dd + " " + getParameterSmoothStartTime(n);
					String toDate = yyyy_mm_dd + " " + getParameterSmoothEndTime(n);
					logger.debug("   #" + n + " [" + fromDate + "] to [" + toDate + "]");
					if (new Formater().isValidDates(startDate, fromDate, toDate, endDate) == false) {
						logger.error("#" + n + " [" + fromDate + "] to [" + toDate + "]");
						throw new Exception(Configuration.class.getName() + ": Invalid dates");
					}
					else {
						new WayPoint().insert(smoothDefinitions, new WayPoint(fromDate, toDate));
					}
				}

				if (flgSmoothCorner == false) {
					if (flgSmoothFromTo == false) {
						throw new Exception(Configuration.class.getName() + ": Invalid Smooth Corner with From/To Lat/Lon");					
					}
					logger.debug("#" + n + " SmoothTopLeftCornerLat [" + smoothTopLeftCornerLat + "] SmoothTopLeftCornerLon [" + smoothTopLeftCornerLon + "]");
					logger.debug("#" + n + " SmoothBottomRightCornerLat [" + smoothBottomRightCornerLat + "] SmoothBottomRightCornerLon [" + smoothBottomRightCornerLon + "]");
					try {
						new WayPoint().insert(smoothDefinitions, new WayPoint(WayPoint.MODE_CORNER, smoothTopLeftCornerLat, smoothTopLeftCornerLon, smoothBottomRightCornerLat, smoothBottomRightCornerLon));
					}
					catch (Exception e) {
						logger.error(e.getMessage());
						throw e;
					}
				}

				if (flgSmoothFromTo == false) {
					logger.debug("#" + n + " SmoothFromLat [" + smoothFromLat + "] SmoothFromLon [" + smoothFromLon + "]");
					logger.debug("#" + n + " SmoothToLat [" + smoothToLat + "] SmoothToLon [" + smoothToLon + "]");
					try {
						WayPoint waypoint = new WayPoint(WayPoint.MODE_FROM_TO, smoothFromLat, smoothFromLon, smoothToLat, smoothToLon);
						waypoint.setSmoothFromToPrecision(getParameterSmoothFromToPrecision(n));
						new WayPoint().insert(smoothDefinitions, waypoint);
					}
					catch (Exception e) {
						logger.error(e.getMessage());
						throw e;
					}
				}
			}
			// Traces etendues
			loggerExtend.info("");
			loggerExtend.info("List of the " + nbr + " Date eor Lat/Lon ranges smoothed sorted");
			n = 0;
			for (WayPoint waypoint: smoothDefinitions) {
				if (waypoint.getDateFrom() != null) {
					//loggerExtend.info("   #" + n + " Dates          [" + waypoint.getDateFrom() + "] to [" + waypoint.getDateTo() + "]");				
				}
				else if (Double.isNaN(waypoint.getSmoothTopLeftCornerLat()) != true) {
					/*
					loggerExtend.info("   #" + n + " Corner Lat/Lon [" + waypoint.getSmoothTopLeftCornerLat() + "]/[" + waypoint.getSmoothTopLeftCornerLon()
					                                        + "] to [" + waypoint.getSmoothBottomRightCornerLat() + "]/[" + waypoint.getSmoothBottomRightCornerLon() + "]");
					*/
				}
				else if (Double.isNaN(waypoint.getSmoothFromLat()) != true) {
					loggerExtend.info("   #" + n + " From   Lat/Lon [" + waypoint.getSmoothFromLat() + "]/[" + waypoint.getSmoothFromLon()
					                                        + "] to [" + waypoint.getSmoothToLat() + "]/[" + waypoint.getSmoothToLon()
					                                        + "] Precision [" + waypoint.getSmoothTFromToPrecision() + "]");
				}
				n++;
			}

			nbr = getParameterNbrWaypoints();
			logger.debug("");
			logger.debug("List of the " + nbr + " time ranges for waypoints");
			n = 0;
			for (n = 0; n < nbr; n++) {
				String name = getParameterWaypointName(n);
				String desc = getParameterWaypointDescription(n);
				String time = getParameterWaypointTime(n);
				double lat = getParameterWaypointLat(n);
				double lon = getParameterWaypointLon(n);
				logger.debug("Name [" + name + "] Desc. [" + desc + "] Time [" + time + "] Lat [" + lat + "] Lon [" + lon + "]");

				// 'time' et ('lat + 'lon' sont exclusifs l'un de l'autre
				if (time != null && (Double.isNaN(lat) == true || Double.isNaN(lon) == true)) {
					// Les 2 'lat' et 'lon' seront renseignes par la record date de 'date' (waypoint sur la trace)
					String date = yyyy_mm_dd + " " + time;
					logger.debug("#" + n + " [" + name + "] [" + desc + "] [" + date + "]");
					if (new Formater().isBetweenDates(startDate, date, endDate) == false) {
						logger.error("#" + n + " [" + name + "] [" + desc + "] [" + date + "]");
						throw new Exception(Configuration.class.getName() + ": Invalid time");
					}
					else {
						new WayPoint().insert(waypoints, new WayPoint(name, desc, date, null));
					}
				}
				else if (Double.isNaN(lat) == false && Double.isNaN(lon) == false) {
					// Les 2 'lat' et 'lon' peuvent etre "a cote" du parcours (waypoint a cote de la trace ;-)
					logger.debug("#" + n + " [" + name + "] [" + desc + "] Lat [" + lat + "] Lon [" + lon + "]");
					new WayPoint().insert(waypoints, new WayPoint(name, desc, lat, lon));
				}
				else {
					throw new Exception(Configuration.class.getName() + ": Missing parameter (time eor (lat/lon))");
				}
			}
			loggerExtend.info("");
			loggerExtend.info("List of the " + nbr + " time ranges for waypoints sorted (before update)");
			n = 0;
			for (WayPoint waypoint: waypoints) {
				loggerExtend.info("   #" + (n++) + " Name [" + waypoint.getName() + "] Desc. [" + waypoint.getDescription() +
					"] Time [" + waypoint.getDateFrom() +
					"] Lat [" + waypoint.getLat() + "] Lon [" + waypoint.getLon() + "]");				
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled() == true) {
				e.printStackTrace();
			}
			throw new Exception(Configuration.class.getName() + ": " + e.getMessage());
		}

	} // getAndTestTimeRanges()

	public Configuration(String dir, String file) throws Exception {

		configPath = dir;
		configFile = file;

		if (configPathFile == null) {
			configPathFile = new File(configPath, configFile);

			// Lecture des parametres generaux
			try {
				traceName = getParameterValue("//Configuration/General/TraceName");;
				product = getParameterValue("//Configuration/General/Product");
				version = getParameterValue("//Configuration/General/Version");

				directory = getParameterDirectory();
				inputFileName = getParameterInputFileName();
				outputFiles = getParameterOutputFiles();
				flgSplit = getParameterFlgSplit();
				flgInverse = getParameterFlgInverse();

				if (flgSplit == false) {
					author = getParameterAuthor();
					description = getParameterDescription();
					trackpointName = getParameterTrackpointName();
					dateTime = getParameterDateTime(true);
					startDate = getParameterStartDate();
					endDate = getParameterEndDate();
					duration = getParameterDuration();

					if (endDate.length() != 0 && duration.length() != 0) {
						throw new Exception(Configuration.class.getName() + ": 'EndDate' and 'Duration' defined for trace " + traceName);
					}
					if (endDate.length() == 0 && duration.length() == 0) {
						throw new Exception(Configuration.class.getName() + ": 'EndDate' and 'Duration' not defined for trace " + traceName);
					}

					scaleLatLonBigDecimal = getParameterScaleLatLonBigDecimal();
					scaleEleBigDecimal = getParameterScaleEleBigDecimal();
					nbrIndentation = getParameterNbrIndentation();
					durationBetweenPeriods = getParameterDurationBetweenPeriods();
					speedThresholdHigh = getParameterSpeedThresholdHigh();
					speedThresholdLow = getParameterSpeedThresholdLow();
					speedTimeoutHigh = getParameterSpeedTimeoutHigh();
					speedTimeoutLow = getParameterSpeedTimeoutLow();
					distBetweenSamples = getParameterDistBetweenSamples();
					flgOutputXml = getParameterFlgOutputXml();
					flgOutputRecords = getParameterFlgOutputRecords();
				}

				// Traces etendues
				LoggerExtend loggerExtend = new LoggerExtend();
				String separatorFile = (configPath.endsWith("/") || configPath.endsWith("\\")) ? "" : ("" + File.separatorChar);
				loggerExtend.info("File configuration [" + configPath + separatorFile + configFile + "]");
				loggerExtend.info("");

				loggerExtend.info("General, meta datas, trackpoint name, etc.");
				loggerExtend.info("   Product         [" + getProduct() + "]");
				loggerExtend.info("   Version         [" + getVersion() + "]");
				loggerExtend.info("   Trace name      [" + this.traceName + "]");

				if (flgSplit == false) {
					loggerExtend.info("   Author          [" + getAuthor() + "]");
					loggerExtend.info("   Description     [" + getDescription() + "]");
					dateTime = getParameterDateTime(true);
					loggerExtend.info("   Production date [" + dateTime + "]");
					loggerExtend.info("   Trackpoint name [" + getTrackpointName() + "]");

					loggerExtend.info("");
					loggerExtend.info("Behavior:");
					loggerExtend.info("   Directory name  [" + directory + "]");
					loggerExtend.info("   Lat/Lon scale   [" + scaleLatLonBigDecimal + "] decimal places (eg " +
						new Formater().bigDecimalToString(new BigDecimal(98.12345678), scaleLatLonBigDecimal) + ")");
					loggerExtend.info("   Ele scale       [" + scaleEleBigDecimal + "] decimal places (eg " +
						new Formater().bigDecimalToString(new BigDecimal(98.12345678), scaleEleBigDecimal) + ")");
					loggerExtend.info("   Indentation     [" + nbrIndentation + "] space characters");

					loggerExtend.info("");
					loggerExtend.info("Filtering:");
					loggerExtend.info("   Between periods [" + durationBetweenPeriods + "] seconds (" +
						new Formater().duration(durationBetweenPeriods) + ")");
					loggerExtend.info("   Speed Thr. high [" + speedThresholdHigh + "] km/h");
					loggerExtend.info("   Speed TO high   [" + speedTimeoutHigh + "] seconds (" +
						new Formater().duration(speedTimeoutHigh) + ")");   
					loggerExtend.info("   Speed Thr. low  [" + speedThresholdLow + "] km/h");
					loggerExtend.info("   Speed TO low    [" + speedTimeoutLow + "] seconds (" +
						new Formater().duration(speedTimeoutLow) + ")");   
					loggerExtend.info("   Dist. samples   [" + distBetweenSamples + "] meters");
				}

				loggerExtend.info("");
				loggerExtend.info("Trace: [" + getTraceName() + "]");
				loggerExtend.info("   Input file name [" + inputFileName + "]");
				loggerExtend.info("   Output files    [" + outputFiles + "]");

				if (flgSplit == false) {
					loggerExtend.info("   Output Xml file [" + ((flgOutputXml == true) ? "Yes" : "No") + "]");
					loggerExtend.info("   Output Rcd file [" + ((flgOutputRecords == true) ? "Yes" : "No") + "]");
					loggerExtend.info("   Start date/time [" + startDate + "]");
					loggerExtend.info("   End   date/time [" + endDate + "]");
					loggerExtend.info("   Duration        [" + duration + "]");
				}
				else {
					loggerExtend.info("   Split files     [Yes]");					
				}

			}  catch (Exception e){
				throw e;	
			}
		}
	}

	public String getProduct() { return product; }
	public String getVersion() { return version; }
	public String getTraceName() { return traceName; }
	public String getDirectory() { return directory; }	

	public String getAuthor() { return author; }
	public String getDescription() { return description; }
	public String getDateTime() { return dateTime; }
	public String getTrackpointName() { return trackpointName; }

	public String getInputFileName() { return inputFileName; }
	public String getStartDate() {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		String startDate2 = new Formater().dateToXmlDate(startDate).toString();
		loggerExtend.info("   Start date/time [" + startDate + "] => [" + startDate2 + "]");
		return startDate2;
	}
	public XMLGregorianCalendar getXmlStartDate() {
		return new Formater().dateToXmlDate(startDate);
	}
	
	public String getEndDate() throws DatatypeConfigurationException {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		String endDate2 = "";
		if (endDate.length() != 0) {
			endDate2 = new Formater().dateToXmlDate(endDate).toString();
			loggerExtend.info("   End   date/time [" + endDate + "] => [" + endDate2 + "]");
		}
		else {
			endDate2 = new Formater().xmlDurationToEndDate(new Formater().dateToXmlDate(startDate).toString(), duration);
			loggerExtend.info("   Duration / End  [" + duration + "] => [" + endDate2 + "]");
			endDate = endDate2;
		}

		// Test de validite des dates Start @ End
		logger.debug("Before date compare (format [YYYY/MM/DD HH:MM:SS])");
		XMLGregorianCalendar dateFrom = new Formater().dateToXmlDate(startDate);
		XMLGregorianCalendar dateTo = new Formater().dateToXmlDate(endDate);
		if (dateTo.compare(dateFrom) == DatatypeConstants.GREATER) {
			logger.debug("Return [" + endDate2 + "] (format [YYYY-MM-DDTHH:MM:SS.000+HH:MM])");
			return endDate2;
		}
		else {
			throw new DatatypeConfigurationException("Invalid 'startDate' @ 'endDate'");
		}
	}
	public XMLGregorianCalendar getXmlEndDate() throws DatatypeConfigurationException {
		return new Formater().dateToXmlDate(endDate);
	}
	
	public String getRecordsFileName() {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		// Fichier de sortie pour une plage horaire (n records valides)
		String name = outputFiles + ".rcd";
		loggerExtend.info("   Out file name   [" + outputFiles + "] => [" + name + "]");
		return name;	
	}

	public String getSplitFileName() {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		// Germe des fichier "splites"
		String name = outputFiles + ".";
		loggerExtend.info("   Split file name [" + outputFiles + "] => [" + name + "YYYYMMDD]");
		return name;	
	}

	public String getXmlFileName() {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		// Fichier xml intermediaire (1! record)
		String name = outputFiles + ".xml";
		loggerExtend.info("   Xml file name   [" + outputFiles + "] => [" + name + "]");
		return name;	
	}

	public String getGpxFileName() {
		// Traces etendues
		LoggerExtend loggerExtend = new LoggerExtend();

		// Fichier gpx a poster (n records)
		String name = outputFiles + ".gpx";
		loggerExtend.info("   Gpx file name   [" + outputFiles + "] => [" + name + "]");
		return name;	
	}

	public int getScaleLatLonBigDecimal() { return scaleLatLonBigDecimal; }
	public int getScaleEleBigDecimal() { return scaleEleBigDecimal; }
	public int getNbrIndentation() { return nbrIndentation; };

	public int getDurationBetweenPeriods() { return durationBetweenPeriods; };
	public double getSpeedThresholdHigh() { return speedThresholdHigh; };
	public double getSpeedThresholdLow() { return speedThresholdLow; };
	public int getSpeedTimeoutHigh () { return speedTimeoutHigh; };
	public int getSpeedTimeoutLow() { return speedTimeoutLow; };
	public int getDistBetweenSamples() { return distBetweenSamples; };

	public boolean getFlgSplit() { return flgSplit; };
	public boolean getFlgOutputXml() { return flgOutputXml; };
	public boolean getFlgOutputRecords() { return flgOutputRecords; };
	public boolean getFlgInverse() { return flgInverse; };

	public List<WayPoint> getCutDates() { return cutDates; };
	public List<WayPoint> getSmoothDefinitions() { return smoothDefinitions; };
	public List<WayPoint> getWaypoints() { return waypoints; };

	public void setConfigPath(String value) { configPath = value; };
	public void setConfigFile(String value) { configFile = value; };

}
