package moscow.mytheria.utility.debug;

import moscow.mytheria.Mytheria;

public class ThreadDebugger {
   public static void logAllThreads() {
      ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();

      ThreadGroup parentGroup;
      while ((parentGroup = rootGroup.getParent()) != null) {
         rootGroup = parentGroup;
      }

      Thread[] threads = new Thread[rootGroup.activeCount() * 2];
      int count = rootGroup.enumerate(threads);
      Mytheria.LOGGER.info("=== Active Threads Debug ===");
      Mytheria.LOGGER.info("Total active threads: {}", count);
      int nonDaemonCount = 0;

      for (int i = 0; i < count; i++) {
         Thread thread = threads[i];
         if (thread != null) {
            boolean isDaemon = thread.isDaemon();
            if (!isDaemon) {
               nonDaemonCount++;
            }

            Mytheria.LOGGER
               .info(
                  "Thread[{}]: name='{}', daemon={}, state={}, alive={}, group={}",
                  new Object[]{
                     i,
                     thread.getName(),
                     isDaemon,
                     thread.getState(),
                     thread.isAlive(),
                     thread.getThreadGroup() != null ? thread.getThreadGroup().getName() : "null"
                  }
               );
         }
      }

      Mytheria.LOGGER.info("Non-daemon threads: {}", nonDaemonCount);
      Mytheria.LOGGER.info("=== End Threads Debug ===");
   }

   public static void interruptAllNonDaemonThreads() {
      ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();

      ThreadGroup parentGroup;
      while ((parentGroup = rootGroup.getParent()) != null) {
         rootGroup = parentGroup;
      }

      Thread[] threads = new Thread[rootGroup.activeCount() * 2];
      int count = rootGroup.enumerate(threads);
      Thread currentThread = Thread.currentThread();

      for (int i = 0; i < count; i++) {
         Thread thread = threads[i];
         if (thread != null && !thread.isDaemon() && thread != currentThread) {
            Mytheria.LOGGER.info("Interrupting non-daemon thread: {} (state: {})", thread.getName(), thread.getState());

            try {
               thread.interrupt();
               thread.join(100L);
            } catch (InterruptedException var8) {
               Mytheria.LOGGER.warn("Interrupted while waiting for thread {} to finish", thread.getName());
            }
         }
      }
   }
}
