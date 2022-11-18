package scannetwork;
import android.content.Context;
import android.util.Log;

import com.github.druk.rx2dnssd.BonjourService;
import com.github.druk.rx2dnssd.Rx2Dnssd;
import com.github.druk.rx2dnssd.Rx2DnssdBindable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**

Be sure to add the line:
	implementation "com.github.andriydruk:rx2dnssd:0.9.16"
to the dependencies in the build.gradle file located at [project]/app/build.gradle

**/

public class MdnsHandler {
    private Rx2Dnssd mRxDnssd;
    private final List<BonjourService> bonjourservices;
    private final long creationTime;
    private Disposable mDisposable;
    public MdnsHandler(Context context) {
        creationTime = System.currentTimeMillis();
        mRxDnssd = new Rx2DnssdBindable(context);
        bonjourservices = Collections.synchronizedList(new ArrayList<>());
        mDisposable = mRxDnssd.browse("_http._tcp", "local.")
                .compose(mRxDnssd.resolve())
                .compose(mRxDnssd.queryIPRecords())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(bonjourService -> {
                    if(bonjourService.getServiceName().startsWith("LR125")) {
                        if(bonjourService.isLost()) {
                            bonjourservices.remove(bonjourService);
                        }
                        else {
                            bonjourservices.add(bonjourService);
                        }
                    }
                }, throwable -> {
                    Log.e("TAG", "error", throwable);
                });
    }

    public List<BonjourService> getServices() {
        final long initTime = 3000;
        if(System.currentTimeMillis() < creationTime + initTime) {
            try {
                Thread.sleep(initTime);
            } catch (InterruptedException e) {
                Log.e("Exception", "Exception in thread sleeping in get services bonjour");
            }
        }
        synchronized (bonjourservices) {
            return new ArrayList<>(bonjourservices);
        }
    }

}
