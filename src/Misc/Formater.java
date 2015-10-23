package Misc;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

public class Formater {

	private static Logger logger = Logger.getLogger(Formater.class);

	private static DatatypeFactory df = null;

	// Methods
	private static String to02d(int n) { return (n < 10)    ? "0" + n        : Integer.valueOf(n).toString(); }
	private static String to03d(int n) { return (n < 100)   ? "0" + to02d(n) : Integer.valueOf(n).toString(); }
	private static String to04d(int n) { return (n < 1000)  ? "0" + to03d(n) : Integer.valueOf(n).toString(); }
	private static String to05d(int n) { return (n < 10000) ? "0" + to04d(n) : Integer.valueOf(n).toString(); }

	// Presentation d'une duree dans le format xxD HH:MM:SS
    public String duration(int time) { return duration(1000L * time); }

    public String duration(long time) {

		boolean flgTimeNeg = false;

		if (time < 0L) {
			time = -time;
			flgTimeNeg = true;
		}

		String formatTime = (flgTimeNeg == true) ? "-" : "";
		long seconds = time / 1000L;

		if (seconds >= 86400L) {
			formatTime = (seconds / 86400) + "D ";
			seconds %= 86400;
		}

		String HH = new Long((seconds / 3600) + 100).toString().substring(1, 3);
		String MM = new Long(((seconds % 3600) / 60) + 100).toString().substring(1, 3);
		String SS = new Long(((seconds % 3600) % 60) + 100).toString().substring(1, 3);
		//String MS = new Long((time % 1000) + 1000).toString().substring(1, 4);

		formatTime = formatTime + HH + ":" + MM + ":" + SS;	 // + "." + MS;

		return formatTime;
	}

    public String dateAndTime() {

    	Date date = new Date(System.currentTimeMillis());
    	String formatDateAndTime = date.toString();
		return formatDateAndTime;
	}

    public String color2Hex(int color) {

    	return Integer.toHexString(0x1000000 + (color & 0xffffff)).toString().substring(1);
    }

    public String character2Hex(Character c) {

    	return Integer.toHexString(0x100 + (c & 0xff)).toString().substring(1);
    }

    public String int2Hex(int value) {

    	return Integer.toHexString(0x1000000 + (value & 0xffffff)).toString().substring(1);
    }

    public void dumpHexa(byte[] datas, int len) {

    	logger.info("Dump of " + len + " bytes (0x" + Integer.toHexString(0x10000 + len).toString().substring(1) + ")");
    	int addrFrom = 0;
    	int addr = 0;
    	String sDatas  = " ";
    	String cDatas  = "  |";
    	String header1 = " 00 01 02 03 04 05 06 07  08 09 0a 0b 0c 0d 0e 0f";
    	String header2 = "  |01234567 89abcdef|";
    	String header = header1 + header2;
    	logger.info("     " + header);
    	int d = 16;
    	for (int n = 0; n < len; n++) {
    		if (n >= 16 && (n % 16) == 0) {
    			String hexAddr = Integer.toHexString(0x10000 + addrFrom).toString().substring(1);
    			logger.info(hexAddr + sDatas + cDatas + "|");
    			sDatas = " ";
    			cDatas = "  |";
    			addrFrom += 16;
    			d = 16;
    		}
    		Byte data = datas[n];
    		String sData = null;
    		char cData = '.';
    		if (data >= 0 && data <= 127) {
    			sData = Integer.toHexString(0x100 + data).toString().substring(1);
    			if (data >= 32 && data < 127) {
    				cData = (char)(0 + data);
    			}
    		}
    		else {
    			sData = Integer.toHexString(0x10000 + data).toString().substring(2);
    		}
    		if (d == 8) {
    			sDatas += " ";
    			cDatas += " ";
    		}
    		sDatas += " " + sData;
    		cDatas += cData;
    		addr += 1;
    		d -= 1;
    	}
    	if (d > 0) {
    		for (int n = 0; n < d; n++) {
    			sDatas += "   ";
    			cDatas += " ";
        		if (n == 8) {
        			sDatas += " ";
        			cDatas += " ";
        		}
    		}
    	}
		String hexAddr = Integer.toHexString(0x10000 + addrFrom).toString().substring(1);
		logger.info(hexAddr + sDatas + cDatas + "|");    	
    }

	public int convColorToEZL176RGB(Color color) {

		// 2 bytes (16 bits = 5 + 6 + 5) define the pixel color in RGB format:
		// R4R3R2R1R0G5G4G3G2G1G0B4B3B2B1B0
		// Where:
		//   msb: R4R3R2R1R0G5G4G3
		//   lsb: G2G1G0B4B3B2B1B0
		return ((color.getRed() >> 3) << (6 + 5)) + ((color.getGreen() >> 2) << 5) + (color.getBlue() >> 3);
	}

	public Color convEZL176RGBToColor(int color) {

		// Padding a 1 sur 8 bits si le bit de poids faible est a 1
		int red   = ((color & 0xf800) >> 8); red   |= ((red & 0x08) != 0x00) ? 0x07 : 0x00;		// Padding 5 => 8 bits
		int green = ((color & 0x07e0) >> 3); green |= ((green & 0x04) != 0x00) ? 0x03 : 0x00;	// Padding 6 => 8 bits
		int blue  = ((color & 0x001f) << 3); blue  |= ((blue & 0x08) != 0x00) ? 0x07 : 0x00;	// Padding 5 => 8 bits

		/*
		System.out.println("Red    0x" + Integer.toHexString(0x100 + red).toString().substring(1) +
						   " Green 0x" + Integer.toHexString(0x100 + green).toString().substring(1) +
						   " Blue  0x" + Integer.toHexString(0x100 + blue).toString().substring(1));
		*/
							return new Color(256 * 256 * red + 256 * green + blue);
	}

	public int string2hex(String str) {

		int value = 0;
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			//System.out.println("#" + i + " [" + chars[i] + "]");
			if (chars[i] <= '9') {
				value = 16 * value + (chars[i] - '0');
			}
			else if (chars[i] >= 'A' && chars[i] <= 'F') {
				value = 16 * value + (10 + (chars[i] - 'A'));
			}
			else if (chars[i] >= 'a' && chars[i] <= 'f') {
				value = 16 * value + (10 + (chars[i] - 'a'));
			}
		}
		return value;
	}

	// Arrondi Double => entier le plus proche
	public int rounding(Double value) {

		int floor = value.intValue();
		Double d = Math.abs(100.0 * value - 100 * floor);
		int celling = d.intValue();
		if (value >= 0.0) {
			// xxx.00, xxx.01, ..., xxx.49 => xxx
			// xxx.50, xxx.51, ..., xxx.99 => (xxx + 1)
			return (celling > 49) ? (floor + 1) : floor;
		}
		else {		
			// -xxx.99, -xxx.98, ..., -xxx.50 => -(xxx + 1)
			// -xxx.49, -xxx.49, ..., -xxx.00 => -xxx
			return (celling > 49) ? (floor - 1) : floor;

		}
	}

    // Conversion d'un double en un string uuu.dd...d avec 'scale' chiffres apres la virgule
    public String doubleToString(Double value, int scale) {
    	String sValue = value.toString();
    	if (sValue.contains(".") == true) {
    		String[] fields = sValue.split("[.]");
    		if (scale <= 0) {
    			sValue = fields[0];
    		}
    		else if (fields[1].length() >= scale) {
    			sValue = fields[0] + "." + fields[1].substring(0, scale);
    		}
    		else {
    			sValue = fields[0] + "." + fields[1].substring(0, fields[1].length());
        		for (int n = fields[1].length(); n < scale; n++) {
        			sValue += "0";
        		}
    		}
    	}
    	else if (scale > 0) {
    		sValue += ".";
    		for (int n = 0; n < scale; n++) {
    			sValue += "0";
    		}
    	}
    	return sValue;
    }

    public String dec2hex(int value) {
    	return Integer.toHexString(0x100 + (value & 0xff)).toString().substring(1);
    }

    public String int2ascii(int value) {
    	String sRtn = "";
    	boolean flgSpace = true;
    	for (int n = 100; n > 0; n /= 10) {
    		int chiffre = (value / n) % 10;
    		if (n > 1 && flgSpace == true) {
    			sRtn += (n > 1 && chiffre != 0) ? " " + dec2hex('0' + chiffre) : " 20";		// ' ' si '0'
    			if (chiffre != 0) {
    				flgSpace = false;
    			}
    		}
    		else {
    			sRtn += " " + dec2hex('0' + chiffre);		// Toujours dernier '0' terminal  			
    		}
    	}
    	return sRtn;
    }

    // Conversion d'un big decimal en un string uuu.dd...d avec 'scale' chiffres apres la virgule
    public String bigDecimalToString(BigDecimal bigdec, int scale) {
    	String sValue = bigdec.toString();
    	if (sValue.contains(".") == true) {
    		String[] fields = sValue.split("[.]");
    		if (scale <= 0) {
    			sValue = fields[0];
    		}
    		else if (fields[1].length() >= scale) {
    			sValue = fields[0] + "." + fields[1].substring(0, scale);
    		}
    		else {
    			sValue = fields[0] + "." + fields[1].substring(0, fields[1].length());
        		for (int n = fields[1].length(); n < scale; n++) {
        			sValue += "0";
        		}
    		}
    	}
    	else if (scale > 0) {
    		sValue += ".";
    		for (int n = 0; n < scale; n++) {
    			sValue += "0";
    		}
    	}
    	return sValue;
    }

    public String getToday(boolean flgAddTZ) {

		GregorianCalendar gCalendar = new GregorianCalendar();

		if (flgAddTZ == true) {
			// Diminution de la duree TZ pour le .gpx
			gCalendar.add(GregorianCalendar.MILLISECOND, -gCalendar.get(GregorianCalendar.ZONE_OFFSET));
		}

		String today = gCalendar.get(Calendar.YEAR) + "/" +
		to02d(gCalendar.get(Calendar.MONTH) + 1) + "/" +
		to02d(gCalendar.get(Calendar.DAY_OF_MONTH)) +
		" " +
		to02d(gCalendar.get(Calendar.HOUR_OF_DAY)) + ":" +
		to02d(gCalendar.get(Calendar.MINUTE)) + ":" +
		to02d(gCalendar.get(Calendar.SECOND));

		return today;
    }

	public XMLGregorianCalendar dateToXmlDate(String xmlDate) {

		String date = "";
		// Suppression des caractere ' ' et tabulation en debut et fin
		String loc_date = xmlDate.trim();
		logger.debug("dateToXmlDate([" + xmlDate + "] => [" + loc_date + "])");
		if (loc_date.length() == "YYYY/MM/DD HH:MM:SS".length()) {
			// Format date [YYYY/MM/DD HH:MM:SS]
			String fields[] = loc_date.split("[ ]");
			date =
				fields[0].split("[/]")[0] +		// Year
				fields[0].split("[/]")[1] +		// Month
				fields[0].split("[/]")[2] +		// Date
				" " +
				fields[1].split("[:]")[0] +		// Hours
				fields[1].split("[:]")[1] +		// Minutes
				fields[1].split("[:]")[2];		// Seconds
		}
		else if (loc_date.length() == "YYYYMMDD HHMMSS".length()) {
			// Date deja convertie
		}
		else if (loc_date.length() == "YYYY-MM-DDTHH:MM:SS.000+HH:MM".length()) {
			// Format date [YYYY-MM-DDTHH:MM:SS.000+HH:MM]
		}
		else {
			logger.error("Wrong format 'date' [" + xmlDate + "]");
		}

		try {
			// Dates xml [YYYY-MM-DDTHH:MM:SS.000+HH:MM]
			XMLGregorianCalendar loc_xmlDate = dateToXmlCalendar(loc_date, false);
			// Fix Bug: Warning: Set the month in the range [0...11] ?!.
			//loc_xmlDate.setMonth(loc_xmlDate.getMonth() - 1);
			logger.debug("=> TZ in minutes [" + loc_xmlDate.getTimezone() + "]");
			return loc_xmlDate;
		}
		catch (Exception e) {
			logger.error("Exception [Conversion with 'dateToXmlCalendar()']");
			return null;
		}
	}

	public String xmlDurationToEndDate(String date, String duration) throws DatatypeConfigurationException {
		
		logger.debug("xmlDurationToEndDate([" + date + "], [" + duration + "])");
		String loc_date = "";
		String loc_duration = duration.trim();
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTimeInMillis(0L);	// Force SSS.mS at sss.000
		long seconds = 0L;
		if (loc_duration.length() >= "H:MM".length()) {
			String fields[] = loc_duration.split("[:]");
			seconds = (3600 * Integer.parseInt(fields[0]) + 60 * Integer.parseInt(fields[1]));
			if (date.length() == "YYYYMMDD HHMMSS".length()) {
				gCalendar.set(
					Integer.parseInt(date.split("[ ]")[0].substring(0, 4)),		// Year
					Integer.parseInt(date.split("[ ]")[0].substring(4, 6)) - 1,		// Month
					Integer.parseInt(date.split("[ ]")[0].substring(6, 8)),		// Date
					Integer.parseInt(date.split("[ ]")[1].substring(0, 2)),		// Hours
					Integer.parseInt(date.split("[ ]")[1].substring(2, 4)),		// Minutes
					Integer.parseInt(date.split("[ ]")[1].substring(4, 6)));	// Seconds
			}
			else if (date.length() == "YYYY-MM-DDTHH:MM:SS.000+HH:MM".length()) {
				gCalendar.set(
					Integer.parseInt(date.substring(0, 4)),		// Year
					Integer.parseInt(date.substring(5, 7)) - 1,		// Month
					Integer.parseInt(date.substring(8, 10)),	// Date
					Integer.parseInt(date.substring(11, 13)),	// Hours
					Integer.parseInt(date.substring(14, 16)),	// Minutes
					Integer.parseInt(date.substring(17, 19)));	// Seconds				
			}
		}

		long loc_time = gCalendar.getTimeInMillis() + 1000L * seconds;	// Duration application
		gCalendar.setTimeInMillis(loc_time);
		loc_date = gCalendar.get(Calendar.YEAR) + "/" +
			to02d(gCalendar.get(Calendar.MONTH)) + "/" +
			to02d(gCalendar.get(Calendar.DAY_OF_MONTH)) +
			" " +
			to02d(gCalendar.get(Calendar.HOUR_OF_DAY)) + ":" +
			to02d(gCalendar.get(Calendar.MINUTE)) + ":" +
			to02d(gCalendar.get(Calendar.SECOND));
		logger.debug("xmlDurationToEndDate() [" + loc_date + "]");
		return loc_date;
	}

	public XMLGregorianCalendar dateToXmlCalendar(String date) throws DatatypeConfigurationException, PatternSyntaxException {
	
		return dateToXmlCalendar(date, true);
	}

	public XMLGregorianCalendar dateToXmlCalendar(String date, boolean flgAddTZ) throws DatatypeConfigurationException, PatternSyntaxException {

		logger.debug("dateToXmlCalendar([" + date +
			"]) TZ [" + new GregorianCalendar().get(GregorianCalendar.ZONE_OFFSET) + " mS]");

		String fields[] = date.split("[ ]");
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTimeInMillis(0L);	// Force SSS.mS at sss.000
		//gCalendar.add(Calendar, amount);
		// Determination du type de formatage de la date en argument:
		// Format supportes:
		// - YYYY/MM/DD HH:MM:SS
		// - YYYYMMDD HHMMSS
		if (date.length() == "YYYY/MM/DD HH:MM:SS".length()) {
			gCalendar.set(
				Integer.parseInt(fields[0].split("[/]")[0]),	// Year
				Integer.parseInt(fields[0].split("[/]")[1]) - 1,	// Month
				Integer.parseInt(fields[0].split("[/]")[2]),	// Date
				Integer.parseInt(fields[1].split("[:]")[0]),	// Hours
				Integer.parseInt(fields[1].split("[:]")[1]),	// Minutes
				Integer.parseInt(fields[1].split("[:]")[2]));	// Seconds

			logger.debug("gCalendar [" + gCalendar.toString() + "]");
		}
		else if (date.length() == "YYYYMMDD HHMMSS".length()) {
			gCalendar.set(
				Integer.parseInt(fields[0].substring(0, 4)),	// Year
				Integer.parseInt(fields[0].substring(4, 6)) - 1,	// Month
				Integer.parseInt(fields[0].substring(6, 8)),	// Date
				Integer.parseInt(fields[1].substring(0, 2)),	// Hours
				Integer.parseInt(fields[1].substring(2, 4)),	// Minutes
				Integer.parseInt(fields[1].substring(4, 6)));	// Seconds

			flgAddTZ = false;
		}
		else {
			throw new PatternSyntaxException("Format 'date' not supported [" + date + "]", "[ /:]", -1);
		}
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining DatatypeFactory instance", dce);
        }

        if (flgAddTZ == true) {
        	gCalendar.add(GregorianCalendar.MILLISECOND, gCalendar.get(GregorianCalendar.ZONE_OFFSET));
        }
        XMLGregorianCalendar xmlCalendar = df.newXMLGregorianCalendar(gCalendar);
               
        logger.debug("xmlCalendar.getMonth() [" + xmlCalendar.getMonth() + "]");

        /* Test
        GregorianCalendar gCalendarNow = new GregorianCalendar();
        logger.info("gCalendarNow [" + gCalendarNow.toString() + "]");
        Date dateNow = new Date();
        dateNow.setTime(System.currentTimeMillis());
        gCalendarNow.setTime(dateNow);
        XMLGregorianCalendar xmlCalendarNow = df.newXMLGregorianCalendar(gCalendarNow);
        logger.info("xmlCalendarNow.getMonth() [" + xmlCalendarNow.getMonth() + "]");
        
        // Fin Test */

		return xmlCalendar;
	}

	public XMLGregorianCalendar dateToXmlCalendar(long time) throws DatatypeConfigurationException {

		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTimeInMillis(time);
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining DatatypeFactory instance", dce);
        }

        XMLGregorianCalendar xmlCalendar = df.newXMLGregorianCalendar(gCalendar);

        return xmlCalendar;
	}

	public boolean isValidDates(String date1, String date2, String date3, String date4) {

		XMLGregorianCalendar xDate1 = new Formater().dateToXmlDate(date1);
		XMLGregorianCalendar xDate2 = new Formater().dateToXmlDate(date2);
		XMLGregorianCalendar xDate3 = new Formater().dateToXmlDate(date3);
		XMLGregorianCalendar xDate4 = new Formater().dateToXmlDate(date4);
		return (xDate1.compare(xDate2) == DatatypeConstants.LESSER
		     && xDate2.compare(xDate3) == DatatypeConstants.LESSER
		     && xDate3.compare(xDate4) == DatatypeConstants.LESSER) ? true : false;
	}

	public boolean isBetweenDates(String date1, String date2, String date3) {

		XMLGregorianCalendar xDate1 = new Formater().dateToXmlDate(date1);
		XMLGregorianCalendar xDate2 = new Formater().dateToXmlDate(date2);
		XMLGregorianCalendar xDate3 = new Formater().dateToXmlDate(date3);
		return (xDate1.compare(xDate2) == DatatypeConstants.LESSER
		     && xDate2.compare(xDate3) == DatatypeConstants.LESSER) ? true : false;
	}

	public boolean isBetweenDates(String date1, XMLGregorianCalendar date2, String date3) {

		XMLGregorianCalendar xDate1 = new Formater().dateToXmlDate(date1);
		XMLGregorianCalendar xDate2 = date2;
		XMLGregorianCalendar xDate3 = new Formater().dateToXmlDate(date3);
		return (xDate1.compare(xDate2) == DatatypeConstants.LESSER
		     && xDate2.compare(xDate3) == DatatypeConstants.LESSER) ? true : false;
	}

	public int compareDates(String date1, String date2) {

		XMLGregorianCalendar xDate1 = new Formater().dateToXmlDate(date1);
		XMLGregorianCalendar xDate2 = new Formater().dateToXmlDate(date2);
		return xDate1.compare(xDate2);
	}

	public int compareDates(XMLGregorianCalendar date1, String date2) {

		XMLGregorianCalendar xDate1 = date1;
		XMLGregorianCalendar xDate2 = new Formater().dateToXmlDate(date2);
		return xDate1.compare(xDate2);
	}

	public int compareDates(String date1, XMLGregorianCalendar date2) {

		XMLGregorianCalendar xDate1 = new Formater().dateToXmlDate(date1);
		XMLGregorianCalendar xDate2 = date2;
		return xDate1.compare(xDate2);
	}

}
