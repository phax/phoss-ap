package com.helger.phoss.ap.webapp.job;

import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.helger.base.string.StringHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.reporting.AppReportingHelper;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (SchedulerConfig.class);

  @Override
  public void configureTasks (@NonNull final ScheduledTaskRegistrar aTaskRegistrar)
  {
    if (APCoreConfig.isPeppolReportingScheduled ())
    {
      // Check configuration
      final int nDayOfMonth = APCoreConfig.getPeppolReportingScheduleDayOfMonth ();
      if (nDayOfMonth < 1 || nDayOfMonth > 15)
        throw new IllegalStateException ("The Peppol Reporting Schedule 'day of month' parameter (" +
                                         nDayOfMonth +
                                         ") is invalid. Must be between 1 and 15.");
      final int nHour = APCoreConfig.getPeppolReportingScheduleHour ();
      if (nHour < 0 || nHour > 23)
        throw new IllegalStateException ("The Peppol Reporting Schedule 'hour' parameter (" +
                                         nHour +
                                         ") is invalid. Must be between 0 and 23.");
      final int nMinute = APCoreConfig.getPeppolReportingScheduleMinute ();
      if (nMinute < 0 || nMinute > 59)
        throw new IllegalStateException ("The Peppol Reporting Schedule 'minute' parameter (" +
                                         nMinute +
                                         ") is invalid. Must be between 0 and 59.");

      LOGGER.info ("Scheduling Peppol Reporting job to run monthly on day " +
                   nDayOfMonth +
                   " at " +
                   StringHelper.getLeadingZero (nHour, 2) +
                   ':' +
                   StringHelper.getLeadingZero (nMinute, 2));

      // Schedule task
      final String sCronKey = "0 " + nMinute + " " + nHour + " " + nDayOfMonth + " * *";
      aTaskRegistrar.addCronTask (new CronTask ( () -> {
        LOGGER.info ("Running scheduled creation and sending of Peppol Reporting messages");
        // Use the previous month
        final YearMonth aYearMonth = YearMonth.now ().minusMonths (1);
        AppReportingHelper.createAndSendPeppolReports (aYearMonth);
      }, sCronKey));
    }
    else
      LOGGER.info ("Peppol Reporting job is disabled per configuration");
  }
}
