package com.example.TestMqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

public class Application {

  @Inject private Environment env;

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static ConfigurableApplicationContext failoverContext;

  private static ConfigurableApplicationContext mainContext;

  private static boolean isStandalone = false, isFailover = false;

  /**
   * Initializes jura.
   *
   * <p>Spring profiles can be configured with a program arguments
   * --spring.profiles.active=your-active-profile
   *
   * <p>You can find more information on how profiles work with JHipster on <a
   * href="http://jhipster.github.io/profiles/">http://jhipster.github.io/profiles/</a>.
   */
  @PostConstruct
  public void initApplication() {
//    log.info("Running with Spring profile(s) : {}", Arrays.toString(env.getActiveProfiles()));
//    Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
//    if (activeProfiles.contains(Constants.SPRING_PROFILE_DEVELOPMENT)
//        && activeProfiles.contains(Constants.SPRING_PROFILE_PRODUCTION)) {
//      log.error(
//          "You have misconfigured your application! It should not run "
//              + "with both the 'dev' and 'prod' profiles at the same time.");
//    }
  }

  public static void main(String[] args) throws Exception {

//    if (Arrays.asList(args).contains("standalone")) {
//      isStandalone = true;
//    }
//
//    SpringApplicationBuilder bbuilder =
//        new SpringApplicationBuilder(MgmtContext.class)
//            .properties(args)
//            .banner(new ReceiverBanner("receivers.management.port", "Management Context"));
//
//    failoverContext = bbuilder.run(args);
//
//    FailoverProperties failProps = failoverContext.getBean(FailoverProperties.class);
//
//    if (!failProps.getEnabled()) {
//      start();
//    }
      start();
  }

  public static void start() {
//    ApplicationArguments args = failoverContext.getBean(ApplicationArguments.class);

    Thread thread =
        new Thread(
            () -> {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
              }

              SpringApplicationBuilder bbuilder =
                  new SpringApplicationBuilder(TestMqttApplication.class)
//                      .properties(args.getSourceArgs())
//                      .bannerMode(Mode.OFF)
//                      .child(HttpContext.class)
                      .banner(new ReceiverBanner("receivers.http.port", "HTTP Context"));

              // this has two issues ... 1) HTTPS needs SSL stuff sorted also starting/stopping
              // context will need tweaking...
//              if (!isStandalone) {
//                bbuilder =
//                    bbuilder
//                        .sibling(new Class<?>[] {HttpsContext.class}, args.getSourceArgs())
//                        .banner(new ReceiverBanner("receivers.https.port", "HTTPS Context"));
//              }
//
              mainContext = bbuilder.run();
//
//              try {
//                StatusCheck checker = mainContext.getBean(StatusCheck.class);
//                failoverContext.getBeanFactory().registerSingleton("checker", checker);
//                failoverContext.getBeanFactory().registerSingleton("mainContext", mainContext);
//              } catch (Exception e) {
//                log.info("Primary not running");
//              }
            });

    thread.setDaemon(false);
    thread.start();
  }

  public static void stop() {
    ApplicationArguments args = failoverContext.getBean(ApplicationArguments.class);

    Thread thread =
        new Thread(
            () -> {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
              }
//              if (mainContext != null) {
//                ConfigurableApplicationContext parentContext =
//                    (ConfigurableApplicationContext) mainContext.getParent();
//                mainContext.stop();
//                mainContext.close();
//                parentContext.close();
//                parentContext.close();
//                failoverContext.getBeanFactory().destroyBean(mainContext);
//              }
//              mainContext = null;
            });

    thread.setDaemon(false);
    thread.start();
  }

  public static void restart() {
    stop();
    start();
  }
}
