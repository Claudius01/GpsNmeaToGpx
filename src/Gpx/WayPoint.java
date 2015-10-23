package Gpx;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import Misc.Formater;

public class WayPoint {

	private static Logger logger = Logger.getLogger(WayPoint.class);

	private boolean flgUpdate = false;
	private String name = "";
	private String description = "";
	private String dateFrom = null;
	private String dateTo = null;
	private double lat = Double.NaN;
	private double lon = Double.NaN;
	private double smoothTopLeftCornerLat = Double.NaN;
	private double smoothTopLeftCornerLon = Double.NaN;
	private double smoothBottomRightCornerLat = Double.NaN;
	private double smoothBottomRightCornerLon = Double.NaN;
	private double smoothFromLat = Double.NaN;
	private double smoothFromLon = Double.NaN;
	private double smoothToLat = Double.NaN;
	private double smoothToLon = Double.NaN;
	private double smoothFromToPrecision = 0.00005;
	private boolean flgSmoothFromTo = false;

	static final int MODE_CORNER = 0, MODE_FROM_TO = 1;
	
	public WayPoint() { }

	public WayPoint(String dateFrom, String dateTo) {
		this.dateFrom = dateFrom;
		this.dateTo = dateTo;
	}

	public WayPoint(String name, String description, String dateFrom, String dateTo) {
		this.name = name;
		this.description = description;
		this.dateFrom = dateFrom;
		this.dateTo = dateTo;
	}

	public WayPoint(String name, String description, double lat, double lon) {
		this.name = name;
		this.description = description;
		this.lat = lat;
		this.lon = lon;
	}

	public WayPoint(int mode, double lat1, double lon1, double lat2, double lon2) throws Exception {

		switch (mode) {
		case MODE_CORNER:
			this.smoothTopLeftCornerLat = lat1;
			this.smoothTopLeftCornerLon = lon1;
			this.smoothBottomRightCornerLat = lat2;
			this.smoothBottomRightCornerLon = lon2;
			break;
		case MODE_FROM_TO:
			this.smoothFromLat = lat1;
			this.smoothFromLon = lon1;
			this.smoothToLat = lat2;
			this.smoothToLon = lon2;
			break;
		default:
			throw new Exception("WayPoint(): Invalid mode: [" + mode + "] not supported"); 
		}
	}

	public void insert(List<WayPoint> list, WayPoint waypoint) {
		
		if (list.size() == 0) {
			list.add(waypoint);
		}
		else {
			list.add(waypoint);		// Insertion en fin de liste
			for (int idx = 0; idx < list.size(); idx++) {
				if (list.get(idx).getDateFrom() != null && waypoint.getDateFrom() != null) {
					if (new Formater().compareDates(list.get(idx).getDateFrom(), waypoint.getDateFrom()) == DatatypeConstants.GREATER) {
						// Decalage de la liste
						int idxInsert = idx;
						for (idx = ((list.size() - 1)); idx > idxInsert; idx--) {
							list.set(idx, list.get(idx-1));
						}
						// Reinsertion
						list.set(idxInsert, waypoint);
					break;
				}
				}
			}
		}
	}

	public void optimize(List<WayPoint> list) {

		// Concatenation des tranches horaires qui se chevauchent
		// Attention: Doit se faire sur une liste triee chronologiquement @ dateFrom
		if (list.size() > 1) {
			for (int n = 1; n < list.size(); n++) {
				if (new Formater().isBetweenDates(list.get(n-1).dateFrom, list.get(n).dateFrom, list.get(n-1).getDateTo()) == true) {
					// Le debut de la plage horaire #n est comprise dans la plage precedente
					// => Replacement de la fin de la plage precedente par la fin de la plage horaire #n
					//    et suppression de la plage horaire #n
				}
			}
		}
	}

	public boolean isBetweenDates(String date) {
		return new Formater().isBetweenDates(dateFrom, date, dateTo);
	}

	public boolean isBetweenDates(XMLGregorianCalendar date) {
		if (dateFrom != null && dateTo != null) {
			return new Formater().isBetweenDates(dateFrom, date, dateTo);
		}
		return false;
	}

	public boolean isBetweenCornerLatLon(BigDecimal lat, BigDecimal lon) {
		// Suppression si dans la zone rectangulaire (Ok si hemisphere Nord a l'Ouest de Caen ;-)
		if (Double.isNaN(smoothTopLeftCornerLat) != true && Double.isNaN(smoothTopLeftCornerLon) != true && Double.isNaN(smoothBottomRightCornerLat) != true && Double.isNaN(smoothBottomRightCornerLon) != true) {
			if ((smoothTopLeftCornerLat >= lat.doubleValue()) && (lat.doubleValue() >= smoothBottomRightCornerLat)
			 && (smoothTopLeftCornerLon <= lon.doubleValue()) && (lon.doubleValue() <= smoothBottomRightCornerLon)) {
				return true;
			}
		}
		return false;
	}

	public boolean isBetweenFromToLatLon(BigDecimal lat, BigDecimal lon) {
		// Suppression si passage dans le From jusqu'au passage dans le To (Ok si hemisphere Nord a l'Ouest de Caen ;-)
		if (Double.isNaN(smoothFromLat) != true && Double.isNaN(smoothFromLon) != true && Double.isNaN(smoothToLat) != true && Double.isNaN(smoothToLon) != true) {
			// 1 minute d'arc est egal a (40 000 km / 360 / 60) ~= 1852 metres
			// Lat/Lon de 1.0 correspond a un degre = 60' correspond a 111120 metres
			// => Lat/Lon de 0.00005 correspond a (111120 * 0.00005) ~= 5 metres
			if (flgSmoothFromTo == false
				&& (smoothFromLat >= lat.doubleValue() - smoothFromToPrecision) && (smoothFromLon <= lon.doubleValue() + smoothFromToPrecision)
				&& (smoothFromLat <= lat.doubleValue() + smoothFromToPrecision) && (smoothFromLon >= lon.doubleValue() - smoothFromToPrecision)) {

				flgSmoothFromTo = true;
				logger.info("");
				logger.info("isBetweenFromToLatLon(): Start with [" + lat + "]/[" + lon + "]");
				return true;
			}
			if (flgSmoothFromTo == true) {
				if ((smoothToLat >= lat.doubleValue() - smoothFromToPrecision) && (smoothToLon <= lon.doubleValue() + smoothFromToPrecision)
				 && (smoothToLat <= lat.doubleValue() + smoothFromToPrecision) && (smoothToLon >= lon.doubleValue() - smoothFromToPrecision)) {

					flgSmoothFromTo = false;
					logger.info("isBetweenFromToLatLon(): End  with [" + lat + "]/[" + lon + "]");
					return false;
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	public boolean getFlgUpdate() { return flgUpdate; }
	public void setFlgUpdate(boolean value) { flgUpdate = value; }

	public String getName() { return name; }
	public String getDescription() { return description; }
	public String getDateFrom() { return dateFrom; }
	public String getDateTo() { return dateTo; }

	public double getLat() { return lat; }
	public double getLon() { return lon; }
	public void setLat(double value) { lat = value; }
	public void setLon(double value) { lon = value; }

	public double getSmoothTopLeftCornerLat() { return smoothTopLeftCornerLat; }
	public double getSmoothTopLeftCornerLon() { return smoothTopLeftCornerLon; }
	public double getSmoothBottomRightCornerLat() { return smoothBottomRightCornerLat; }
	public double getSmoothBottomRightCornerLon() { return smoothBottomRightCornerLon; }

	public double getSmoothFromLat() { return smoothFromLat; }
	public double getSmoothFromLon() { return smoothFromLon; }
	public double getSmoothToLat() { return smoothToLat; }
	public double getSmoothToLon() { return smoothToLon; }
	public double getSmoothTFromToPrecision() { return smoothFromToPrecision; }

	public void setSmoothFromToPrecision(double smoothFromToPrecision) {
		this.smoothFromToPrecision = smoothFromToPrecision;
	}

}
