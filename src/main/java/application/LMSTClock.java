package application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jastronomy.jsofa.JSOFA;

public class LMSTClock {
	
	public static String toHHMMSS(double degValue){
		int hours = (int)(degValue/15.0);
		int minutes = (int)((degValue - hours*15.0)*60/15.0);
		double seconds = (degValue - (hours + minutes/60.0)*15.0)*3600/15.0;
		int intSeconds = (int)Math.round(seconds);
		String hhmmss = String.format("%02d:%02d:%02d", hours,minutes,intSeconds);
		return hhmmss;
	}
	
	private static double getGAST (double mjd){	// convert MJD in UT1 to a LAST

		final int num_leap_seconds = 26;
		final double   tt_offset = 32.184;

		double dt = (double) num_leap_seconds + tt_offset;
		double jd_base = 2400000.5;

		JSOFA.JulianDate terrestrialTime = JSOFA.jauUt1tt (jd_base, mjd, dt);
		if (terrestrialTime == null)
		{
			System.err.println("ERROR in UT1 to TT conversion\n");
			return 0;
		}

		double gast = JSOFA.jauGst06a (jd_base, mjd, terrestrialTime.djm0, terrestrialTime.djm1);
		return gast;
	}
	
	public static double getRadLMST (double mjd, double degLong)
	{
		double gast, last;

		gast = getGAST (mjd);
		last  = gast + degLong * Math.PI/180.0;

		final double two_pi = 2.0 * Math.PI;
		double w = last % two_pi;
		return ( w >= 0.0 ) ? w : w + two_pi;

	}
	
	public static double getDegLMST(double mjd, double degLongitude) {
		return getRadLMST(mjd, degLongitude) * 180.0 / Math.PI;
	}
	static CommandLineParser parser = new DefaultParser();
	static Options options = new Options();
	static CommandLine commandLine;
	
	public static void main(String[] args) {
		Double degLong = null;
		boolean iterate = false;

		Option telescope = new Option("t","telescope",true, "telescope name: Parkes / MeerKAT / Molonglo");
		Option longitude = new Option("l","longitude",true, "telescope longitude in degrees");
		Option clockMode = new Option("c","clock_mode",false, "run in clock mode: iterate every second");
		Option help = new Option("h","help",false, "show this help message");
		options.addOption(telescope);
		options.addOption(longitude);
		options.addOption(help);
		options.addOption(clockMode);
		
		try {
			commandLine = parser.parse(options, args);
			if(hasOption(help)){
				help();
				System.exit(0);
			}
			if(hasOption(clockMode)) {
				iterate=true;
			}
			if(hasOption(longitude)) {
				String value = getValue(longitude);
				degLong = Double.parseDouble(value);
			}
			else if(hasOption(telescope)) {
				String value = getValue(telescope).toLowerCase();
				switch(value) {
				case "meerkat":
					degLong=21.4430;
					break;
				case "parkes":
					degLong=148.2621;
					break;
				case "molonglo":
					degLong=149.424658;
					break;
					
					
				
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
			
		do {
			LocalDateTime nowDateTime = LocalDateTime.now(Clock.systemUTC());
			Double nowMJD = nowDateTime.getLong(JulianFields.MODIFIED_JULIAN_DAY)
					+(nowDateTime.getHour()*3600
					 + nowDateTime.getMinute()*60 
					 + nowDateTime.getSecond()
					 )/86400.0;
			
			try {	
				Double degLMST = getDegLMST(nowMJD, degLong);
				System.err.print("UTC:" + getUTCString(nowDateTime) + " LMST:"+ toHHMMSS(degLMST));
				System.err.print("\r");
				Thread.sleep(1000);
				nowMJD = nowMJD + 1/86400.0;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}while(iterate);
		
		
	}
	
	public static boolean hasOption(Option option){
		return commandLine.hasOption(option.getOpt());
	}

	public static String getValue(Option option){
		return commandLine.getOptionValue(option.getOpt());
	}

	public static void help(){
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("LMSTClock", options);
	}
	public static String getUTCString(LocalDateTime utcTime){
		return utcTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-kk:mm:ss.SSS"));
	}
}
