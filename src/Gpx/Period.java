package Gpx;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import Misc.Formater;

public class Period {

	private static Logger logger = Logger.getLogger(Period.class);
	private static String to02d(int n) { return (n < 10)    ? "0" + n        : Integer.valueOf(n).toString(); }

	// Attributs permettant de construire une periode avec:
	// Remarque: Ces atributs sont a raz avant toute nouvelle campagne de recherche
	private static int gId = 0;							// son Id
	private static long gBeginningTime = 0L;			// sa date de debut
	private static long gEndingTime = 0L;				// sa date de fin
	private static long gPreviousTime = 0L;				// sa date precedente pour connaitre les ecarts
	private static int gNbrSamples = 0;					// son nombre d'echantillons
	private static int gCurrentSpeedTimeoutHigh = 0;	// Timeout courant
	private static int gCurrentSpeedTimeoutLow = 0;		// Timeout courant
	private static double gSpeedAverage = 0.0;			// Moyenne de la vitesse dans la periode

	private static List<Period> listPeriodsCriteriaDuration = new ArrayList<Period>();
	private static List<Period> listPeriodsCriteriaSpeed = new ArrayList<Period>();

	private static int durationBetweenPeriods = 0;	// Duree separant 2 periodes distinctes en secondes
	private static double speedThresholdHigh = 0.0;	// Seuil haut vitesse en km/h pour une meme periode
	private static double speedThresholdLow = 0.0;	// Seuil bas vitesse en km/h pour une meme periode
	private static int speedTimeoutHigh = 0;		// Timeout passage au dessus de 'speedThresholdHigh'
	private static int speedTimeoutLow = 0;			// Timeout passage en deca de 'speedThresholdLow'

	private int id = 0;							// Id
	private long beginningTime = 0L;			// Date de debut
	private long endingTime = 0L;				// Date de fin
	private int nbrSamples = 0;					// Nombre d'echantillons
	private double speedAverage = 0.0;			// Moyenne de la vitesse dans la periode
	private boolean flgPeriodComplete = false;

	public Period() { }			// Pour manipuler les attributs globaux

	// Determination des periodes separees d'au moins 'durationBetweenPeriods' secondes
	// => Permet de se reperer dans un unique enregistrement pris a des temps differents
	public Period(long time) throws Exception {

		logger.debug("Period([" + time + "] => [" + new Formater().dateToXmlCalendar(time) + "])");
		id = gId;

		if (gBeginningTime == 0L) {
			gBeginningTime = time;
		}
		else if (time >= gPreviousTime) {
			long diff = (time - gPreviousTime);
			if (diff > (1000L * durationBetweenPeriods)) {
				gEndingTime = gPreviousTime;
				nbrSamples = gNbrSamples;
			}
		}
		else {
			logger.error("Sample #" + gNbrSamples + " Back to the future [" +
				new Formater().dateToXmlCalendar(time) + " => [" +
				new Formater().dateToXmlCalendar(gPreviousTime) + "]");
				throw new Exception("Abort...");
		}
		gNbrSamples++;
		gPreviousTime = time;

		if (gBeginningTime != 0L && gEndingTime != 0L) {
			logger.debug("Period #" + id + " Beginning [" +
				new Formater().dateToXmlCalendar(gBeginningTime) + "] Ending [" +
				new Formater().dateToXmlCalendar(gEndingTime) + "] Duration [" +
				new Formater().duration(gEndingTime - gBeginningTime) + "] Samples [" +
				nbrSamples + "]");

			// Memorisation de la periode avec ses caracteristiques
			beginningTime = gBeginningTime;
			endingTime = gEndingTime;
			flgPeriodComplete = true;
			listPeriodsCriteriaDuration.add(this);

			// Commencement d'une nouvelle periode
			gBeginningTime = time;
			gEndingTime = 0L;
			gNbrSamples = 1;		// 1er echantillon de la periode
			gId++;
		}
	}

	// Determination des periodes pour lesquelles la vitesse est superieure a 'speedThresholdHigh' km/h avec hysteresis
	// A chaque passage au dessus / en deca des seuils Haut / bas, le timeout est armee
	// A l'expiration de ce timeout, la periode est commencee ou completee si vitesse > 'speedThresholdHigh'
	// A l'expiration de ce timeout, la periode est terminee si vitesse < 'speedThresholdLow'
	// => Permet de reperer dans une periode les differentes phases du parcours (debut, regime etabli, pause et fin)
	public Period(long time, Double speed) throws Exception {

		id = gId;

		if (speed == null) {
			// Fin de la periode en cours si celle-ci est commencee
			if (gBeginningTime != 0L) {
				logger.warn("Force end of Period @speed #" + id + " with [" +
					new Formater().dateToXmlCalendar(time) + "]");
				gEndingTime = time;
				nbrSamples = ++gNbrSamples;
				logger.debug("Period @speed #" + id + " Beginning [" +
					new Formater().dateToXmlCalendar(gBeginningTime) + "] Ending [" +
					new Formater().dateToXmlCalendar(gEndingTime) + "] Duration [" +
					new Formater().duration(gEndingTime - gBeginningTime) + "] Samples [" +
					nbrSamples + "]");

				// Memorisation de la periode avec ses caracteristiques
				beginningTime = gBeginningTime;
				endingTime = gEndingTime;
				speedAverage = gSpeedAverage;
				flgPeriodComplete = true;
				listPeriodsCriteriaSpeed.add(this);
			}
			return;
		}

		logger.debug("Period([" + time + "] => [" + new Formater().dateToXmlCalendar(time) + "], [" + speed + "] km/h)");

		int deltaTime = (gPreviousTime != 0) ? (int)((time - gPreviousTime) / 1000L) : 1;
		if (deltaTime < 0) {
			logger.error("Sample #" + gNbrSamples + " Back to the future [" +
					new Formater().dateToXmlCalendar(time) + " => [" +
					new Formater().dateToXmlCalendar(gPreviousTime) + "]");
					throw new Exception("Abort...");
		}

		// Records au dessus du seuil haut
		if (speed >= speedThresholdHigh) {
			logger.debug("Sample #" + gNbrSamples + " Period [" +
					new Formater().dateToXmlCalendar(time) + "] [" + speed + "] >= [" + speedThresholdHigh +
					"] Timeout high [" + gCurrentSpeedTimeoutHigh + "]");

			if (gCurrentSpeedTimeoutHigh >= speedTimeoutHigh) {
				// Vitesse haute stable pendant la duree attendue
				if (gBeginningTime == 0L) {
					// Debut de la periode
					gBeginningTime = time;
					logger.debug("Sample #" + gNbrSamples + " Start period [" +
							new Formater().dateToXmlCalendar(gBeginningTime) + "]");
					gSpeedAverage = speed;
				}
			}
			else {				
				if (gCurrentSpeedTimeoutLow > 0L) {
					gCurrentSpeedTimeoutLow -= deltaTime;
				}
				gCurrentSpeedTimeoutHigh += deltaTime;
			}
		}

		// Records en deca du seuil bas
		else if (speed <= speedThresholdLow) {
			logger.debug("Sample #" + gNbrSamples + " Period [" +
					new Formater().dateToXmlCalendar(time) + "] [" + speed + "] <= [" + speedThresholdLow +
					"] Timeout low [" + gCurrentSpeedTimeoutLow + "]");

			if (gCurrentSpeedTimeoutLow >= speedTimeoutLow) {
				// Vitesse basse stable pendant la duree attendue
				if (gBeginningTime != 0L && gEndingTime == 0L) {
					// Fin de la periode
					gEndingTime = time;
					nbrSamples = gNbrSamples;
					logger.debug("Sample #" + gNbrSamples + " End period [" +
							new Formater().dateToXmlCalendar(gEndingTime) + "]");
				}
			}
			else {
				if (gCurrentSpeedTimeoutHigh > 0L) {
					gCurrentSpeedTimeoutHigh -= deltaTime;
				}
				gCurrentSpeedTimeoutLow += deltaTime;				
			}
		}

		gPreviousTime = time;

		if (gBeginningTime != 0L) {
			logger.debug("Sample [" + gNbrSamples + "] => [" + new Formater().dateToXmlCalendar(time) +
				"] speed / avg [" + speed + "]/[" + gSpeedAverage + "] km/h");
			gNbrSamples++;

			// Moyenne et Ecart type (algorithme incremental)
			gSpeedAverage = ((gNbrSamples - 1) * gSpeedAverage + speed) / gNbrSamples;

			if (gEndingTime != 0L) {
				logger.debug("Period @speed #" + id + " Beginning [" +
						new Formater().dateToXmlCalendar(gBeginningTime) + "] Ending [" +
						new Formater().dateToXmlCalendar(gEndingTime) + "] Duration [" +
						new Formater().duration(gEndingTime - gBeginningTime) + "] Samples [" +	nbrSamples +
						"] Speed average [" + new Formater().doubleToString(gSpeedAverage, 1) + " km/h]");

				// Memorisation de la periode avec ses caracteristiques
				beginningTime = gBeginningTime;
				endingTime = gEndingTime;
				speedAverage = gSpeedAverage;
				flgPeriodComplete = true;
				listPeriodsCriteriaSpeed.add(this);

				// Commencement d'une nouvelle periode
				gBeginningTime = 0L;
				gEndingTime = 0L;
				gNbrSamples = 0;
				gId++;
			}
		}
	}

	public int getDurationBetweenPeriods() { return durationBetweenPeriods; }
	public void setDurationBetweenPeriods(int duration) { durationBetweenPeriods = duration; }

	public void setSpeedProperties(double speedThresholdHigh, double speedThresholdLow, int speedTimeoutHigh, int speedTimeoutLow) {
		this.speedThresholdHigh = speedThresholdHigh;
		this.speedThresholdLow = speedThresholdLow;
		this.speedTimeoutHigh = speedTimeoutHigh;
		this.speedTimeoutLow = speedTimeoutLow;
	}

	public List<Period> getListPeriodsCriteriaDuration() { return listPeriodsCriteriaDuration; }
	public List<Period> getListPeriodsCriteriaSpeed() { return listPeriodsCriteriaSpeed; }
	public int getId() { return id; }
	public long getBeginningTime() { return beginningTime; }
	public long getEndingTime() { return endingTime; }
	public long getDuration() { return (endingTime - beginningTime); }
	public int getNbrSamples() { return nbrSamples; }
	public boolean isPeriodComplete() { return flgPeriodComplete; }
	public double getSpeedAverage() { return speedAverage; };

	// Remise a zero des proprietes pour une nouvelle campagne de recherche de periodes
	// avec d'autre criteres (revient a raz les attributs globaux sual les listes ;-)
	public void resetProperties() {	
		gId = 0;
		gBeginningTime = 0L;
		gEndingTime = 0L;
		gPreviousTime = 0L;
		gNbrSamples = 0;
		gCurrentSpeedTimeoutHigh = 0;
		gCurrentSpeedTimeoutLow = 0;
		gSpeedAverage = 0.0;
	}

	public static void main(String args[]) {

		try {
			// Test #0
			logger.info("#0 Test with a date YYYY/MM/DD HH:MM:SS setting");
			GregorianCalendar gCalendar = new GregorianCalendar();
			String today =
				gCalendar.get(GregorianCalendar.YEAR) + "/" +
				to02d(gCalendar.get(GregorianCalendar.MONTH)) + "/" +
				to02d(gCalendar.get(GregorianCalendar.DAY_OF_MONTH)) + " " +
				to02d(gCalendar.get(GregorianCalendar.HOUR_OF_DAY)) + ":" +
				to02d(gCalendar.get(GregorianCalendar.MINUTE)) + ":" +
				to02d(gCalendar.get(GregorianCalendar.SECOND));

			logger.info("   Today [" + today + "]");
			XMLGregorianCalendar xToday = new Formater().dateToXmlCalendar(today);
			logger.info("   => [" + xToday + "]");
			logger.info("   => [" + xToday.normalize() + "] (normalized - format expected by .gpx)");

			// Test #1
			logger.info("#1 Test with a date mS setting");
			long timeToday = xToday.toGregorianCalendar().getTimeInMillis();	// Today in mS
			long timeTomorrow = (timeToday + 86400L * 1000);					// Tomorrow in mS
			XMLGregorianCalendar xTomorrow = new Formater().dateToXmlCalendar(timeTomorrow);
			logger.info("   Tomorrow [" + timeTomorrow + "] mS");
			logger.info("   => [" + xTomorrow + "]");
			logger.info("   => [" + xTomorrow.normalize() + "] (normalized - format expected by .gpx)");

			// Test #2
			logger.info("#2 Test of dates comparison (tomorrow is ...");
			int diff = xTomorrow.compare(xToday);
			switch (diff) {
			case DatatypeConstants.LESSER:
				logger.info("   LESSER");
				break;
			case DatatypeConstants.EQUAL:
				logger.info("   EQUAL");
				break;
			case DatatypeConstants.GREATER:
				logger.info("   GREATER");
				break;
			case DatatypeConstants.INDETERMINATE:
				logger.info("   INDETERMINATE");
				break;
			default:
				logger.error("");
				break;
			}
			logger.info("   ... at today ;-)");

		} catch (Exception e) {
			StackTraceElement[] stacks = e.getStackTrace();
			for (StackTraceElement message: stacks) {
				logger.error(message.toString());
			}
        }
	}

}
