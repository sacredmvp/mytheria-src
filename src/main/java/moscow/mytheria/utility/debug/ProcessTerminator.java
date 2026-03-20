package moscow.mytheria.utility.debug;

import java.util.concurrent.TimeUnit;
import moscow.mytheria.Mytheria;

public class ProcessTerminator {
   private static volatile boolean terminationInProgress = false;

   public static void terminateProcess() {
      if (!terminationInProgress) {
         terminationInProgress = true;
         Mytheria.LOGGER.info("Starting aggressive process termination");
         System.err.println("[ProcessTerminator] Starting aggressive process termination");
         Thread terminator = new Thread(() -> {
            long pid = ProcessHandle.current().pid();
            System.err.println("[ProcessTerminator] Current PID: " + pid);
            System.err.println("[ProcessTerminator] Attempting taskkill /F /T /PID " + pid);
            executeCommand("taskkill", "/F", "/T", "/PID", String.valueOf(pid));

            try {
               Thread.sleep(100L);
            } catch (InterruptedException var5) {
            }

            System.err.println("[ProcessTerminator] Attempting taskkill /F /PID " + pid);
            executeCommand("taskkill", "/F", "/PID", String.valueOf(pid));

            try {
               System.err.println("[ProcessTerminator] Attempting ProcessHandle.destroyForcibly()");
               ProcessHandle.current().destroyForcibly();
            } catch (Exception var4) {
               Mytheria.LOGGER.warn("ProcessHandle.destroyForcibly failed: {}", var4.getMessage());
            }

            try {
               System.err.println("[ProcessTerminator] Attempting System.exit(0)");
               System.exit(0);
            } catch (Exception var3) {
               Mytheria.LOGGER.warn("System.exit failed: {}", var3.getMessage());
            }

            System.err.println("[ProcessTerminator] Attempting Runtime.halt(0)");
            Runtime.getRuntime().halt(0);
         }, "Process-Terminator");
         terminator.setDaemon(false);
         terminator.setPriority(10);
         terminator.start();
         System.err.println("[ProcessTerminator] Terminator thread started");
      }
   }

   public static void scheduleTermination(int delayMs) {
      Mytheria.LOGGER.info("Scheduling termination in {}ms", delayMs);
      Thread scheduler = new Thread(() -> {
         try {
            Mytheria.LOGGER.info("Termination scheduler started, waiting {}ms", delayMs);
            Thread.sleep(delayMs);
            Mytheria.LOGGER.error("Scheduled termination triggered after {}ms", delayMs);
            terminateProcess();
         } catch (InterruptedException var2) {
            Mytheria.LOGGER.error("Termination scheduler interrupted, terminating immediately");
            Thread.currentThread().interrupt();
            terminateProcess();
         }
      }, "Termination-Scheduler");
      scheduler.setDaemon(false);
      scheduler.setPriority(10);
      scheduler.start();
      Mytheria.LOGGER.info("Termination scheduler thread started: {}", scheduler.getName());
   }

   private static void executeCommand(String... command) {
      try {
         ProcessBuilder pb = new ProcessBuilder(command);
         pb.redirectErrorStream(true);
         Process process = pb.start();
         boolean finished = process.waitFor(500L, TimeUnit.MILLISECONDS);
         if (finished) {
            Mytheria.LOGGER.info("Command executed: {}", String.join(" ", command));
         } else {
            Mytheria.LOGGER.warn("Command timed out: {}", String.join(" ", command));
            process.destroyForcibly();
         }
      } catch (Exception var4) {
         Mytheria.LOGGER.warn("Failed to execute command {}: {}", String.join(" ", command), var4.getMessage());
      }
   }
}
