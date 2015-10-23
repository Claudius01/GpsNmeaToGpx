package Gpx;

import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import Misc.Formater;

import com.topografix.gpx.BoundsType;
import com.topografix.gpx.GpxType;
import com.topografix.gpx.MetadataType;
import com.topografix.gpx.PersonType;
import com.topografix.gpx.TrkType;
import com.topografix.gpx.TrksegType;
import com.topografix.gpx.WptType;

public class Xml {

	private static Logger logger = Logger.getLogger(Xml.class);
	private static int forceWinterTime = 0;		// 0, 1 (force + 1st) => 2 (force + no trave)

	private GpxType gpxType = null;
	private MetadataType metadataType = null; 
	private TrkType trkType = null;
	private TrksegType trksegType = null;
	private Configuration config = null;

	public Xml(Configuration config) {

		logger.debug("   Initialization and set configuration");
		gpxType = new GpxType();
		this.config = config;

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
	}

	// Initialisation des meta donnees
	/*
		<metadata>
    		<author>Author</author>
    		<desc>Description</desc>
    		<time>YYYY-MM-DDTHH:MM:SS.000+HH:MM</time>
    		<bounds minlat="+/-uu.dddddd" maxlat="+/-uu.dddddd" minlon="+/-uuu.dddddd" maxlon="+/-uuu.dddddd"/>
  		</metadata>
	 */
	public boolean setMetadata(double minlat, double maxlat, double minlon, double maxlon) throws DatatypeConfigurationException {

		if (gpxType == null) {
			logger.error("Context not initialized");
			return false;
		}
		if (metadataType != null) {
			logger.error("Metadata already initialized");
			return false;
		}

		logger.debug("   Initialization and set metadatas");

		// Metadata
		metadataType = new MetadataType();
		PersonType personType = new PersonType();
		personType.setName(config.getAuthor());
		metadataType.setAuthor(personType);
		metadataType.setDesc(config.getDescription());
		metadataType.setTime(new Formater().dateToXmlCalendar(config.getDateTime()));

		// Bounds
		BoundsType boundsType = new BoundsType();
		boundsType.setMinlat(new BigDecimal(minlat));
		boundsType.setMaxlat(new BigDecimal(maxlat));
		boundsType.setMinlon(new BigDecimal(minlon));
		boundsType.setMaxlon(new BigDecimal(maxlon));
		metadataType.setBounds(boundsType);

		gpxType.setMetadata(metadataType);

		return true;
	}

	// Added a way point
	/*
  		<wpt lat="+/-uu.dddddd" lon="+/-uuu.dddddd">
    		<name>Name of waypoint</name>
    		<desc>Description of this waypoint</desc>
  		</wpt>
	 */
	public boolean addWayPoint(String name, String desc, double lat, double lon) {

		if (gpxType == null) {
			logger.error("Context not initialized");
			return false;
		}

		logger.debug("   Initialization a new waypoint Name [" + name + "] Desc. [" + desc + "]");
		WptType wptType = new WptType();
		wptType.setLat(new BigDecimal(lat));
		wptType.setLon(new BigDecimal(lon));
		wptType.setName(name);
		wptType.setDesc(desc);
		gpxType.getWpt().add(wptType);

		return true;
	}

	// Create a trackpoint with a description
	/*
  		<trk>
    		<name>Traces description</name>
    		<trkseg>
      			...
	 */
	public boolean createTrackPoint(String description) {

		if (gpxType == null) {
			logger.error("Context not initialized");
			return false;
		}

		if (trksegType != null && trkType != null) {
			// Terminaison du dernier 'trackpoint'
			logger.debug("   Finalization the trackpoints collection [" + trkType.getName() + "]");
			trkType.getTrkseg().add(trksegType);
			gpxType.getTrk().add(trkType);
		}

		logger.debug("   Initialization a new trackpoints collection [" + description + "]");

		trkType = new TrkType();
		trkType.setName(description);

		trksegType = new TrksegType();
		return true;
	}

			// Added a trackpoint with a description of its first call
	/*
  		<trk>
    		<name>Traces description</name>
    		<trkseg>
      			<trkpt lat="+/-uu.dddddd" lon="+/-uuu.dddddd">
        			<ele>uuu.d</ele>
        			<time>YYYY-MM-DDTHH:MM:SS.000+HH:MM</time>
      			</trkpt>
      		...	
	 */
	public boolean addTrackPoint(BigDecimal lat, BigDecimal lon, BigDecimal ele, XMLGregorianCalendar time, String comment) {

		if (gpxType == null) {
			logger.error("Context not initialized");
			return false;
		}

		if (trkType == null) {
			logger.debug("   Force the initialization a new trackpoints collection");
			trkType = new TrkType();
			trkType.setName(config.getTrackpointName());

			trksegType = new TrksegType();
		}

		WptType trkpt = new WptType();
		trkpt.setLat(lat);
		trkpt.setLon(lon);
		trkpt.setEle(ele);

		// Workaround: Ete / Hiver
		XMLGregorianCalendar locTime = time;
		if (forceWinterTime > 0) {
			if (forceWinterTime == 1) logger.warn("Force winter time ;-)");
			locTime.setHour(time.getHour() - 1);
			forceWinterTime = 2;
		}
		else {
			locTime.setHour(time.getHour());
		}

		trkpt.setTime(locTime);

		if (comment != null) {
			// Utilisation de 'Cmt' pour passer la ligne de commentaire ;-)
			trkpt.setCmt(comment);
		}
		trksegType.getTrkpt().add(trkpt);

		return true;
	}

	//* Get du contexte pour reprise avant de generer le .gpx
	public GpxType getType() {

		if (gpxType == null) {
			logger.error("Context not initialized");
			return null;
		}
		if (metadataType == null) {
			logger.error("Metadata not initialized");
			return null;
		}
		if (trkType == null) {
			logger.error("Trackpoint not initialized");
			return null;
		}

		logger.debug("   Finalization the trackpoints collection [" + trkType.getName() + "]");

		// Terminaison du dernier 'trackpoint'
		trkType.getTrkseg().add(trksegType);
		gpxType.getTrk().add(trkType);

		logger.debug("   Return the " + gpxType.getTrk().size() + " trackpoints collection");

		return gpxType;
	}

}
