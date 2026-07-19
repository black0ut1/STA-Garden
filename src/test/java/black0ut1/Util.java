package black0ut1;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;

public class Util {
	
	public static void dumpHeap() {
		try {
			HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(),
					"com.sun.management:type=HotSpotDiagnostic",
					HotSpotDiagnosticMXBean.class
			);
			mxBean.dumpHeap("heap.hprof", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
