package moscow.mytheria.utility.debug;

import java.util.concurrent.TimeUnit;
import moscow.mytheria.Mytheria;

public class ForceKill {
   public static void forceKillProcess() {
      try {
         long pid = ProcessHandle.current().pid();
         Mytheria.LOGGER.info("Force killing process with PID: {}", pid);

         try {
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(1000L, TimeUnit.MILLISECONDS);
            Mytheria.LOGGER.info("Taskkill command executed");
         } catch (Exception var5) {
            Mytheria.LOGGER.warn("Taskkill failed: {}", var5.getMessage());
         }

         try {
            ProcessHandle.current().destroy();
            Thread.sleep(100L);
            ProcessHandle.current().destroyForcibly();
            Mytheria.LOGGER.info("ProcessHandle destroy executed");
         } catch (Exception var4) {
            Mytheria.LOGGER.warn("ProcessHandle destroy failed: {}", var4.getMessage());
         }

         System.exit(-1);
      } catch (Exception var6) {
         Mytheria.LOGGER.error("Failed to force kill process", var6);
         Runtime.getRuntime().halt(-1);
      }
   }

   public static void scheduleForceKill(int delayMs) {
      Thread killer = new Thread(() -> {
         try {
            Thread.sleep(delayMs);
            Mytheria.LOGGER.error("Force kill timer expired, terminating process");
            forceKillProcess();
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
            forceKillProcess();
         }
      });
      killer.setDaemon(false);
      killer.setName("Force-Kill-Timer");
      killer.start();
   }
}
