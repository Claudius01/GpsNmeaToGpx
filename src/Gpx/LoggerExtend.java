package Gpx;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class LoggerExtend {

	private static Logger logger = Logger.getLogger(LoggerExtend.class);

	private static List<String> messages = new ArrayList<String>();

	public void info(String message) {

		logger.info(message);
		messages.add(message);
	}

	public List<String> getMessages() { return messages; }
	public void clear() { messages.clear(); }
}
