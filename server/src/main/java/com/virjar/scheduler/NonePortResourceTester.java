package com.virjar.scheduler;

import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;
import com.virjar.utils.*;

/**
 * 有些时候我们只能得到IP,不能拿到端口,所以通过这个组件来探测端口。原理是从已获取的数据资源中统计可能端口,然后去测试 <br/>
 * Created by virjar on 16/9/15.
 */
@Component
public class NonePortResourceTester implements Runnable, InitializingBean {

    @Resource
    private ProxyRepository proxyRepository;

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    private LinkedBlockingDeque<String> ipTaskQueue = new LinkedBlockingDeque<>();

    private static final Logger logger = LoggerFactory.getLogger(NonePortResourceTester.class);

    private boolean isRunning = false;

    private List<Integer> ports;

    private BloomFilter bloomFilter = new BloomFilter64bit(60000, 10);
    private int addTimes = 0;
    private ExecutorService pool = new ThreadPoolExecutor(SysConfig.getInstance().getPortCheckThread(),
            SysConfig.getInstance().getPortCheckThread(), 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static NonePortResourceTester instance;

    public static boolean sendIp(String ip) {
        if (instance == null) {
            logger.warn("port check component not start,ip add failed");
            return false;
        }
        return instance.addIp(ip);
    }

    public boolean addIp(String ip) {
        if (++addTimes > 6000) {
            bloomFilter = new BloomFilter64bit(60000, 10);
            addTimes = 0;
        }
        return bloomFilter.add(ip) && ipTaskQueue.offer(ip);
    }

    @Override
    public void run() {
        isRunning = true;
        ports = proxyRepository.getPortList();
        if (ports.size() < 100) {// 认为是新启动的系统,执行默认代码
            ports = Lists.newArrayList();
            buildDefaultPort(ports);
        }
        while (isRunning) {
            for (int i = 0; i < 10; i++) {// 每次拿10个IP
                try {
                    String ip = ipTaskQueue.take();
                    List<Future<List<Proxy>>> futures = Lists.newArrayList();
                    futures.add(pool.submit(new PortChecker(ip)));

                    List<Proxy> proxies = Lists.newArrayList();
                    for (Future<List<Proxy>> future : futures) {
                        proxies.addAll(future.get());
                    }
                    ResourceFilter.filter(proxies);
                    proxyService.save(beanMapper.mapAsList(proxies, ProxyModel.class));
                } catch (Exception e) {
                    logger.error("can not take ip from task queue");
                }
            }
        }
    }

    private class PortChecker implements Callable<List<Proxy>> {

        private String ip;

        PortChecker(String ip) {
            this.ip = ip;
        }

        @Override
        public List<Proxy> call() throws Exception {
            return testPort(ip);
        }
    }

    private List<Proxy> testPort(String ip) {
        List<Proxy> ret = Lists.newArrayList();
        for (Integer port : ports) {
            if (BooleanUtils.isTrue(ProxyUtil.validateProxyConnect(new HttpHost(ip, port)))) {
                Proxy proxy = new Proxy();
                proxy.setIp(ip);
                proxy.setPort(port);
                proxy.setIpValue(ProxyUtil.toIPValue(ip));
                proxy.setSource("portTester");
                ret.add(proxy);
            }
        }
        return ret;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
        instance = this;
    }

    private void buildDefaultPort(List<Integer> ports) {
        ports.add(8080);
        ports.add(8090);
        ports.add(3128);
        ports.add(80);
        ports.add(8888);
        ports.add(8123);
        ports.add(9000);
        ports.add(8998);
        ports.add(9999);
        ports.add(8118);
        ports.add(8088);
        ports.add(9797);
        ports.add(18186);
        ports.add(6666);
        ports.add(6675);
        ports.add(808);
        ports.add(9064);
        ports.add(81);
        ports.add(6673);
        ports.add(6668);
        ports.add(8000);
        ports.add(21320);
        ports.add(8083);
        ports.add(1080);
        ports.add(55336);
        ports.add(8081);
        ports.add(8089);
        ports.add(63000);
        ports.add(87);
        ports.add(443);
        ports.add(8085);
        ports.add(3129);
        ports.add(4444);
        ports.add(82);
        ports.add(8585);
        ports.add(9090);
        ports.add(8292);
        ports.add(9529);
        ports.add(7808);
        ports.add(20000);
        ports.add(25);
        ports.add(83);
        ports.add(37564);
        ports.add(8898);
        ports.add(8082);
        ports.add(1920);
        ports.add(1111);
        ports.add(6667);
        ports.add(8003);
        ports.add(3130);
        ports.add(54321);
        ports.add(10000);
        ports.add(84);
        ports.add(6670);
        ports.add(7777);
        ports.add(1337);
        ports.add(666);
        ports.add(6588);
        ports.add(8086);
        ports.add(843);
        ports.add(18000);
        ports.add(85);
        ports.add(6672);
        ports.add(8800);
        ports.add(8001);
        ports.add(14560);
        ports.add(5555);
        ports.add(6671);
        ports.add(49385);
        ports.add(60000);
        ports.add(86);
        ports.add(8087);
        ports.add(6515);
        ports.add(1234);
        ports.add(10080);
        ports.add(88);
        ports.add(8181);
        ports.add(6674);
        ports.add(2226);
        ports.add(3127);
        ports.add(8909);
        ports.add(7004);
        ports.add(8008);
        ports.add(91);
        ports.add(92);
        ports.add(14555);
        ports.add(7780);
        ports.add(6669);
        ports.add(89);
        ports.add(90);
        ports.add(8102);
        ports.add(10200);
        ports.add(8899);
        ports.add(93);
        ports.add(8125);
        ports.add(60088);
        ports.add(42321);
        ports.add(8119);
        ports.add(8182);
        ports.add(8254);
        ports.add(8129);
        ports.add(8101);
        ports.add(3333);
        ports.add(8360);
        ports.add(8128);
        ports.add(8817);
        ports.add(12345);
        ports.add(1998);
        ports.add(3131);
        ports.add(8908);
        ports.add(95);
        ports.add(9080);
        ports.add(4045);
        ports.add(2020);
        ports.add(7166);
        ports.add(8104);
        ports.add(8084);
        ports.add(9001);
        ports.add(6006);
        ports.add(6969);
        ports.add(8468);
        ports.add(8127);
        ports.add(8122);
        ports.add(31281);
        ports.add(8559);
        ports.add(9876);
        ports.add(8856);
        ports.add(96);
        ports.add(9011);
        ports.add(8146);
        ports.add(8115);
        ports.add(9027);
        ports.add(8877);
        ports.add(8920);
        ports.add(9048);
        ports.add(8139);
        ports.add(8143);
        ports.add(8197);
        ports.add(8137);
        ports.add(8095);
        ports.add(8518);
        ports.add(94);
        ports.add(8105);
        ports.add(8910);
        ports.add(9050);
        ports.add(8091);
        ports.add(8808);
        ports.add(8429);
        ports.add(8106);
        ports.add(8112);
        ports.add(8989);
        ports.add(8787);
        ports.add(8812);
        ports.add(8708);
        ports.add(24000);
        ports.add(8673);
        ports.add(8791);
        ports.add(8705);
        ports.add(8748);
        ports.add(8152);
        ports.add(8562);
        ports.add(8158);
        ports.add(8092);
        ports.add(8570);
        ports.add(8094);
        ports.add(8617);
        ports.add(8356);
        ports.add(8206);
        ports.add(8831);
        ports.add(8186);
        ports.add(8757);
        ports.add(8649);
        ports.add(8124);
        ports.add(8432);
        ports.add(8715);
        ports.add(8232);
        ports.add(8233);
        ports.add(8130);
        ports.add(8736);
        ports.add(21193);
        ports.add(8955);
        ports.add(8552);
        ports.add(8543);
        ports.add(8100);
        ports.add(9069);
        ports.add(8098);
        ports.add(8126);
        ports.add(8131);
        ports.add(8598);
        ports.add(8099);
        ports.add(8548);
        ports.add(8103);
        ports.add(8280);
        ports.add(8918);
        ports.add(18080);
        ports.add(8741);
        ports.add(8630);
        ports.add(8523);
        ports.add(8425);
        ports.add(8759);
        ports.add(2222);
        ports.add(8345);
        ports.add(8689);
        ports.add(8218);
        ports.add(8772);
        ports.add(8506);
        ports.add(8890);
        ports.add(8923);
        ports.add(8300);
        ports.add(8214);
        ports.add(8161);
        ports.add(8801);
        ports.add(8737);
        ports.add(8279);
        ports.add(8784);
        ports.add(8301);
        ports.add(8445);
        ports.add(8447);
        ports.add(8245);
        ports.add(8988);
        ports.add(8303);
        ports.add(8421);
        ports.add(8313);
        ports.add(8566);
        ports.add(8287);
        ports.add(8317);
        ports.add(8284);
        ports.add(8203);
        ports.add(8676);
        ports.add(8966);
        ports.add(8448);
        ports.add(8510);
        ports.add(8901);
        ports.add(8861);
        ports.add(8202);
        ports.add(8285);
        ports.add(8371);
        ports.add(8937);
        ports.add(8258);
        ports.add(8781);
        ports.add(8780);
        ports.add(9055);
        ports.add(8156);
        ports.add(8584);
        ports.add(8803);
        ports.add(8257);
        ports.add(8805);
        ports.add(8466);
        ports.add(8532);
        ports.add(8475);
        ports.add(8819);
        ports.add(8587);
        ports.add(9043);
        ports.add(8863);
        ports.add(8108);
        ports.add(8418);
        ports.add(8136);
        ports.add(9046);
        ports.add(9047);
        ports.add(8779);
        ports.add(8541);
        ports.add(8827);
        ports.add(8929);
        ports.add(8221);
        ports.add(8493);
        ports.add(8841);
        ports.add(8643);
        ports.add(8451);
        ports.add(8223);
        ports.add(8121);
        ports.add(8438);
        ports.add(8964);
        ports.add(8470);
        ports.add(8573);
        ports.add(8745);
        ports.add(8722);
        ports.add(8889);
        ports.add(8239);
        ports.add(8132);
        ports.add(8963);
        ports.add(8553);
        ports.add(8560);
        ports.add(8389);
        ports.add(8246);
        ports.add(8484);
        ports.add(8954);
        ports.add(8174);
        ports.add(8825);
        ports.add(8114);
        ports.add(8456);
        ports.add(8871);
        ports.add(8378);
        ports.add(8191);
        ports.add(9053);
        ports.add(9062);
        ports.add(8878);
        ports.add(8837);
        ports.add(8879);
        ports.add(8222);
        ports.add(8922);
        ports.add(8155);
        ports.add(8821);
        ports.add(8950);
        ports.add(8145);
        ports.add(8762);
        ports.add(9023);
        ports.add(8961);
        ports.add(8388);
        ports.add(8886);
        ports.add(8270);
        ports.add(8694);
        ports.add(9067);
        ports.add(8839);
        ports.add(8995);
        ports.add(8611);
        ports.add(8372);
        ports.add(8687);
        ports.add(8903);
        ports.add(8799);
        ports.add(8442);
        ports.add(18628);
        ports.add(8282);
        ports.add(8160);
        ports.add(8236);
        ports.add(8138);
        ports.add(8531);
        ports.add(8154);
        ports.add(8882);
        ports.add(8724);
        ports.add(8426);
        ports.add(8266);
        ports.add(53);
        ports.add(9009);
        ports.add(8168);
        ports.add(8521);
        ports.add(8608);
        ports.add(21);
        ports.add(9049);
        ports.add(8325);
        ports.add(8683);
        ports.add(8850);
        ports.add(8542);
        ports.add(8558);
        ports.add(8605);
        ports.add(8144);
        ports.add(8595);
        ports.add(8844);
        ports.add(8395);
        ports.add(8897);
        ports.add(8159);
        ports.add(8977);
        ports.add(8939);
        ports.add(8135);
        ports.add(8188);
        ports.add(8777);
        ports.add(8117);
        ports.add(8190);
        ports.add(8710);
        ports.add(8858);
        ports.add(8991);
        ports.add(9075);
        ports.add(9059);
        ports.add(8312);
        ports.add(8339);
        ports.add(8513);
        ports.add(8459);
        ports.add(8707);
        ports.add(8876);
        ports.add(8483);
        ports.add(8620);
        ports.add(8225);
        ports.add(8528);
        ports.add(8120);
        ports.add(8189);
        ports.add(8423);
        ports.add(8526);
        ports.add(9014);
        ports.add(8497);
        ports.add(8228);
        ports.add(8911);
        ports.add(8621);
        ports.add(8157);
        ports.add(9030);
        ports.add(8688);
        ports.add(8591);
        ports.add(8351);
        ports.add(8322);
        ports.add(56419);
        ports.add(8373);
        ports.add(8294);
        ports.add(8763);
        ports.add(8593);
        ports.add(8809);
        ports.add(8244);
        ports.add(8607);
        ports.add(8196);
        ports.add(8252);
        ports.add(8930);
        ports.add(8500);
        ports.add(8974);
        ports.add(8973);
        ports.add(8782);
        ports.add(8455);
        ports.add(8859);
        ports.add(8768);
        ports.add(8842);
        ports.add(8171);
        ports.add(4040);
        ports.add(50000);
        ports.add(8318);
        ports.add(8365);
        ports.add(8544);
        ports.add(8994);
        ports.add(8756);
        ports.add(8985);
        ports.add(8491);
        ports.add(8382);
        ports.add(8586);
        ports.add(8638);
        ports.add(8477);
        ports.add(3000);
        ports.add(29786);
        ports.add(8640);
        ports.add(8107);
        ports.add(8555);
        ports.add(8652);
        ports.add(8149);
        ports.add(8674);
        ports.add(8235);
        ports.add(8942);
        ports.add(8308);
        ports.add(8247);
        ports.add(8659);
        ports.add(8324);
        ports.add(8655);
        ports.add(8662);
        ports.add(8927);
        ports.add(8167);
        ports.add(8588);
        ports.add(8428);
        ports.add(8576);
        ports.add(8733);
        ports.add(8864);
        ports.add(8524);
        ports.add(8391);
        ports.add(8854);
        ports.add(8341);
        ports.add(9042);
        ports.add(59345);
        ports.add(8880);
        ports.add(8631);
        ports.add(8590);
        ports.add(8635);
        ports.add(8946);
        ports.add(9004);
        ports.add(8207);
        ports.add(8150);
        ports.add(8487);
        ports.add(8527);
        ports.add(8376);
        ports.add(8743);
        ports.add(8580);
        ports.add(8999);
        ports.add(9070);
        ports.add(8272);
        ports.add(8945);
        ports.add(8437);
        ports.add(8274);
        ports.add(8905);
        ports.add(8227);
        ports.add(8096);
        ports.add(8775);
        ports.add(8667);
        ports.add(8657);
        ports.add(8255);
        ports.add(8473);
        ports.add(8699);
        ports.add(8380);
        ports.add(8278);
        ports.add(8948);
        ports.add(8276);
        ports.add(8286);
        ports.add(8818);
        ports.add(8342);
        ports.add(8618);
        ports.add(8471);
        ports.add(8349);
        ports.add(8951);
        ports.add(8440);
        ports.add(8180);
        ports.add(8533);
        ports.add(8786);
        ports.add(8358);
        ports.add(9010);
        ports.add(8853);
        ports.add(8987);
        ports.add(8328);
        ports.add(8329);
        ports.add(8778);
        ports.add(8752);
        ports.add(8935);
        ports.add(8976);
        ports.add(8496);
        ports.add(8485);
        ports.add(8795);
        ports.add(8925);
        ports.add(8661);
        ports.add(8316);
        ports.add(8958);
        ports.add(8401);
        ports.add(8581);
        ports.add(33942);
        ports.add(8162);
        ports.add(8824);
        ports.add(8370);
        ports.add(8240);
        ports.add(8326);
        ports.add(8582);
        ports.add(8628);
        ports.add(8614);
        ports.add(8572);
        ports.add(8444);
        ports.add(8807);
        ports.add(8896);
        ports.add(8393);
        ports.add(8461);
        ports.add(8333);
        ports.add(9008);
        ports.add(8335);
        ports.add(8405);
        ports.add(8296);
        ports.add(8721);
        ports.add(8250);
        ports.add(8732);
        ports.add(8387);
        ports.add(9031);
        ports.add(8924);
        ports.add(13475);
        ports.add(8873);
        ports.add(8260);
        ports.add(9002);
        ports.add(8690);
        ports.add(8517);
        ports.add(8902);
        ports.add(8992);
        ports.add(8172);
        ports.add(8478);
        ports.add(8192);
        ports.add(8299);
        ports.add(8504);
        ports.add(9061);
        ports.add(8097);
        ports.add(24745);
        ports.add(8311);
        ports.add(9052);
        ports.add(8868);
        ports.add(8183);
        ports.add(8411);
        ports.add(9024);
        ports.add(8711);
        ports.add(8700);
        ports.add(8433);
        ports.add(800);
        ports.add(8208);
        ports.add(8540);
        ports.add(8654);
        ports.add(9040);
        ports.add(8519);
        ports.add(8953);
        ports.add(8776);
        ports.add(8845);
        ports.add(8467);
        ports.add(8785);
        ports.add(9078);
        ports.add(8343);
        ports.add(8193);
        ports.add(8408);
        ports.add(8151);
        ports.add(8217);
        ports.add(8734);
        ports.add(8793);
        ports.add(8952);
        ports.add(8653);
        ports.add(2013);
        ports.add(8166);
        ports.add(8169);
        ports.add(8702);
        ports.add(8551);
        ports.add(8938);
        ports.add(8494);
        ports.add(8681);
        ports.add(8941);
        ports.add(9073);
        ports.add(8606);
        ports.add(8248);
        ports.add(8835);
        ports.add(8561);
        ports.add(8242);
        ports.add(8569);
        ports.add(8310);
        ports.add(8770);
        ports.add(8599);
        ports.add(8430);
        ports.add(8347);
        ports.add(8642);
        ports.add(9012);
        ports.add(8321);
        ports.add(888);
        ports.add(8678);
        ports.add(8481);
        ports.add(8656);
        ports.add(8111);
        ports.add(8840);
        ports.add(8400);
        ports.add(8109);
        ports.add(8334);
        ports.add(9015);
        ports.add(8386);
        ports.add(8729);
        ports.add(9029);
        ports.add(8892);
        ports.add(8735);
        ports.add(8993);
        ports.add(8133);
        ports.add(8211);
        ports.add(8379);
        ports.add(8891);
        ports.add(8446);
        ports.add(8212);
        ports.add(8749);
        ports.add(8790);
        ports.add(8332);
        ports.add(9044);
        ports.add(8981);
        ports.add(10001);
        ports.add(9022);
        ports.add(8277);
        ports.add(8463);
        ports.add(8949);
        ports.add(8788);
        ports.add(8874);
        ports.add(8739);
        ports.add(8915);
        ports.add(9033);
        ports.add(9054);
        ports.add(9018);
        ports.add(8298);
        ports.add(8666);
        ports.add(8488);
        ports.add(8727);
        ports.add(8249);
        ports.add(8717);
        ports.add(8814);
        ports.add(8822);
        ports.add(8816);
        ports.add(8230);
        ports.add(8414);
        ports.add(8917);
        ports.add(8668);
        ports.add(8289);
        ports.add(1071);
        ports.add(8971);
        ports.add(8627);
        ports.add(8828);
        ports.add(13579);
        ports.add(8525);
        ports.add(8684);
        ports.add(8574);
        ports.add(8224);
        ports.add(8441);
        ports.add(8612);
        ports.add(9028);
        ports.add(8625);
        ports.add(8538);
        ports.add(8947);
        ports.add(8712);
        ports.add(8210);
        ports.add(9058);
        ports.add(8875);
        ports.add(8914);
        ports.add(8431);
        ports.add(8315);
        ports.add(8881);
        ports.add(33948);
        ports.add(8904);
        ports.add(9013);
        ports.add(8747);
        ports.add(8215);
        ports.add(8529);
        ports.add(8632);
        ports.add(8178);
        ports.add(8476);
        ports.add(8367);
        ports.add(9909);
        ports.add(8723);
        ports.add(8893);
        ports.add(8670);
        ports.add(8110);
        ports.add(8281);
        ports.add(8771);
        ports.add(9026);
        ports.add(8698);
        ports.add(8604);
        ports.add(8512);
        ports.add(8836);
        ports.add(8362);
        ports.add(3120);
        ports.add(8934);
        ports.add(8453);
        ports.add(8660);
        ports.add(100);
        ports.add(8693);
        ports.add(55555);
        ports.add(8623);
        ports.add(8113);
        ports.add(8912);
        ports.add(23);
        ports.add(8975);
        ports.add(8725);
        ports.add(8583);
        ports.add(8720);
        ports.add(8597);
        ports.add(9003);
        ports.add(8921);
        ports.add(8834);
        ports.add(8829);
        ports.add(8979);
        ports.add(8419);
        ports.add(8293);
        ports.add(8093);
        ports.add(8499);
        ports.add(8550);
        ports.add(8761);
        ports.add(8959);
        ports.add(8344);
        ports.add(8838);
        ports.add(8165);
        ports.add(8603);
        ports.add(8337);
        ports.add(8458);
        ports.add(8564);
        ports.add(8619);
        ports.add(8651);
        ports.add(8754);
        ports.add(9072);
        ports.add(8216);
        ports.add(8534);
        ports.add(8755);
        ports.add(8701);
        ports.add(8234);
        ports.add(8177);
        ports.add(8134);
        ports.add(8375);
        ports.add(8986);
        ports.add(8633);
        ports.add(8802);
        ports.add(8200);
        ports.add(8851);
        ports.add(8383);
        ports.add(8201);
        ports.add(8271);
        ports.add(8403);
        ports.add(8610);
        ports.add(8984);
        ports.add(8502);
        ports.add(8297);
        ports.add(8965);
        ports.add(8972);
        ports.add(8515);
        ports.add(9039);
        ports.add(9037);
        ports.add(8237);
        ports.add(8792);
        ports.add(8509);
        ports.add(8394);
        ports.add(8692);
        ports.add(8462);
        ports.add(8679);
        ports.add(8957);
        ports.add(8507);
        ports.add(8452);
        ports.add(8199);
        ports.add(8900);
        ports.add(9021);
        ports.add(8490);
        ports.add(8148);
        ports.add(8765);
        ports.add(8810);
        ports.add(8746);
        ports.add(9041);
        ports.add(8742);
        ports.add(8596);
        ports.add(8615);
        ports.add(9066);
        ports.add(8213);
        ports.add(8469);
        ports.add(8355);
        ports.add(8830);
        ports.add(9100);
        ports.add(8187);
        ports.add(8637);
        ports.add(8238);
        ports.add(8330);
        ports.add(37377);
        ports.add(9016);
        ports.add(8340);
        ports.add(8396);
        ports.add(8501);
        ports.add(99);
        ports.add(8141);
        ports.add(8716);
        ports.add(8184);
        ports.add(8010);
        ports.add(8820);
        ports.add(8465);
        ports.add(8399);
        ports.add(8848);
        ports.add(8860);
        ports.add(8589);
        ports.add(8268);
        ports.add(8936);
        ports.add(8767);
        ports.add(8460);
        ports.add(8813);
        ports.add(9019);
        ports.add(8940);
        ports.add(8422);
        ports.add(8980);
        ports.add(8970);
        ports.add(8718);
        ports.add(8295);
        ports.add(8601);
        ports.add(8142);
        ports.add(8357);
        ports.add(8482);
        ports.add(8417);
        ports.add(8368);
        ports.add(8402);
        ports.add(8664);
        ports.add(8390);
        ports.add(8354);
        ports.add(8492);
        ports.add(8454);
        ports.add(8926);
        ports.add(37381);
        ports.add(8522);
        ports.add(8435);
        ports.add(8804);
        ports.add(8797);
        ports.add(8565);
        ports.add(8758);
        ports.add(33128);
        ports.add(8520);
        ports.add(8253);
        ports.add(8195);
        ports.add(8413);
        ports.add(8671);
        ports.add(8549);
        ports.add(8867);
        ports.add(8220);
        ports.add(8634);
        ports.add(8997);
        ports.add(8480);
        ports.add(22);
        ports.add(8219);
        ports.add(8919);
        ports.add(17403);
        ports.add(54186);
        ports.add(8916);
        ports.add(8872);
        ports.add(8933);
        ports.add(8852);
        ports.add(8439);
        ports.add(8398);
        ports.add(8503);
        ports.add(8450);
        ports.add(8592);
        ports.add(2214);
        ports.add(19305);
        ports.add(8226);
        ports.add(8567);
        ports.add(8907);
        ports.add(8291);
        ports.add(8305);
        ports.add(8243);
        ports.add(8397);
        ports.add(8204);
        ports.add(8479);
        ports.add(14826);
        ports.add(8457);
        ports.add(8983);
        ports.add(8895);
        ports.add(8639);
        ports.add(8474);
        ports.add(8798);
        ports.add(8744);
        ports.add(13669);
        ports.add(8990);
        ports.add(8677);
        ports.add(9074);
        ports.add(8738);
        ports.add(8323);
        ports.add(110);
        ports.add(8409);
        ports.add(8170);
        ports.add(9025);
        ports.add(8392);
        ports.add(8264);
        ports.add(8163);
        ports.add(8530);
        ports.add(9006);
        ports.add(8198);
        ports.add(8563);
        ports.add(8866);
        ports.add(8943);
        ports.add(9005);
        ports.add(8704);
        ports.add(21894);
        ports.add(8982);
        ports.add(8826);
        ports.add(8153);
        ports.add(8956);
        ports.add(8663);
        ports.add(6863);
        ports.add(2015);
        ports.add(8906);
        ports.add(8760);
        ports.add(8602);
        ports.add(8978);
        ports.add(8369);
        ports.add(9063);
        ports.add(8641);
        ports.add(8594);
        ports.add(8796);
        ports.add(8164);
        ports.add(8353);
        ports.add(8862);
        ports.add(8645);
        ports.add(8290);
        ports.add(8352);
        ports.add(8870);
        ports.add(8650);
        ports.add(8140);
        ports.add(8424);
        ports.add(59454);
        ports.add(8968);
        ports.add(8361);
        ports.add(8773);
        ports.add(8913);
        ports.add(8412);
        ports.add(8962);
        ports.add(9077);
        ports.add(59385);
        ports.add(18001);
        ports.add(8302);
        ports.add(8273);
        ports.add(8647);
        ports.add(8672);
        ports.add(8843);
        ports.add(8420);
        ports.add(8251);
        ports.add(9007);
        ports.add(8624);
        ports.add(8535);
        ports.add(8928);
        ports.add(8229);
        ports.add(8536);
        ports.add(8556);
        ports.add(50317);
        ports.add(8682);
        ports.add(98);
        ports.add(8932);
        ports.add(9065);
        ports.add(1180);
        ports.add(8706);
        ports.add(9091);
        ports.add(8259);
        ports.add(8855);
        ports.add(8406);
        ports.add(8578);
        ports.add(8833);
        ports.add(8175);
        ports.add(8846);
        ports.add(1453);
        ports.add(18888);
        ports.add(8697);
        ports.add(8209);
        ports.add(8495);
        ports.add(9079);
        ports.add(8849);
        ports.add(8147);
        ports.add(8554);
        ports.add(8811);
        ports.add(8616);
        ports.add(8685);
        ports.add(8750);
        ports.add(8427);
        ports.add(8307);
        ports.add(1212);
        ports.add(9179);
        ports.add(17183);
        ports.add(9071);
        ports.add(9076);
        ports.add(8546);
        ports.add(7000);
        ports.add(8288);
        ports.add(8327);
        ports.add(8646);
        ports.add(8537);
        ports.add(8769);
        ports.add(8262);
        ports.add(8568);
        ports.add(1081);
        ports.add(8346);
        ports.add(8514);
        ports.add(8511);
        ports.add(8669);
        ports.add(3123);
        ports.add(9527);
        ports.add(29037);
        ports.add(23684);
        ports.add(8764);
        ports.add(8887);
        ports.add(8404);
        ports.add(8364);
        ports.add(8857);
        ports.add(8275);
        ports.add(8269);
        ports.add(8265);
        ports.add(8967);
        ports.add(8443);
        ports.add(8314);
        ports.add(8931);
        ports.add(8613);
        ports.add(15692);
        ports.add(1818);
        ports.add(8644);
        ports.add(8545);
        ports.add(8869);
        ports.add(8815);
        ports.add(15238);
        ports.add(29832);
        ports.add(34032);
        ports.add(8695);
        ports.add(4567);
        ports.add(8410);
        ports.add(8256);
        ports.add(8884);
        ports.add(8783);
        ports.add(8728);
        ports.add(8366);
        ports.add(8176);
        ports.add(9045);
        ports.add(8359);
        ports.add(9038);
        ports.add(33719);
        ports.add(8883);
        ports.add(8304);
        ports.add(8377);
        ports.add(4624);
        ports.add(3124);
        ports.add(23944);
        ports.add(34484);
        ports.add(8577);
        ports.add(8374);
        ports.add(8179);
        ports.add(8751);
        ports.add(9034);
        ports.add(37380);
        ports.add(8766);
        ports.add(41425);
        ports.add(1000);
        ports.add(8609);
        ports.add(9068);
        ports.add(7080);
        ports.add(31288);
        ports.add(8434);
        ports.add(8385);
        ports.add(58739);
        ports.add(19279);
        ports.add(9032);
        ports.add(8267);
        ports.add(8944);
        ports.add(8336);
        ports.add(8885);
        ports.add(59349);
        ports.add(8363);
        ports.add(8231);
        ports.add(8713);
        ports.add(19058);
        ports.add(8331);
        ports.add(8486);
        ports.add(8571);
        ports.add(53636);
        ports.add(8416);
        ports.add(8472);
        ports.add(8665);
        ports.add(8557);
        ports.add(8832);
        ports.add(8449);
        ports.add(8714);
        ports.add(8691);
        ports.add(8740);
        ports.add(59346);
        ports.add(8505);
        ports.add(3030);
        ports.add(8173);
        ports.add(33965);
        ports.add(8241);
        ports.add(21724);
        ports.add(109);
        ports.add(8012);
        ports.add(8338);
        ports.add(20121);
        ports.add(55012);
        ports.add(23836);
        ports.add(8320);
        ports.add(8823);
        ports.add(8753);
        ports.add(57166);
        ports.add(24322);
        ports.add(8498);
        ports.add(29359);
        ports.add(123);
        ports.add(6789);
        ports.add(13374);
        ports.add(1209);
        ports.add(8675);
        ports.add(9060);
        ports.add(3389);
        ports.add(8731);
        ports.add(2010);
        ports.add(24809);
        ports.add(43411);
        ports.add(2000);
        ports.add(818);
        ports.add(56220);
        ports.add(9051);
        ports.add(809);
        ports.add(1907);
        ports.add(8194);
        ports.add(8719);
        ports.add(8636);
        ports.add(8319);
        ports.add(8309);
        ports.add(9415);
        ports.add(18350);
        ports.add(6060);
        ports.add(8865);
        ports.add(16158);
        ports.add(8806);
        ports.add(8263);
        ports.add(9393);
        ports.add(999);
        ports.add(7769);
        ports.add(23775);
        ports.add(33919);
        ports.add(8116);
        ports.add(33925);
        ports.add(16515);
        ports.add(8579);
        ports.add(31035);
        ports.add(8516);
        ports.add(8847);
        ports.add(8658);
        ports.add(8464);
        ports.add(8703);
        ports.add(17945);
        ports.add(18253);
        ports.add(34015);
        ports.add(8185);
        ports.add(8407);
        ports.add(8306);
        ports.add(8789);
        ports.add(8622);
        ports.add(8283);
        ports.add(39900);
        ports.add(20);
        ports.add(20771);
        ports.add(18256);
        ports.add(9035);
        ports.add(9190);
        ports.add(8696);
        ports.add(105);
        ports.add(59347);
        ports.add(58688);
        ports.add(43489);
        ports.add(9988);
        ports.add(1200);
        ports.add(1010);
        ports.add(23685);
        ports.add(25085);
        ports.add(10);
        ports.add(2080);
        ports.add(97);
        ports.add(29988);
        ports.add(9056);
        ports.add(3021);
        ports.add(59348);
        ports.add(4000);
        ports.add(10983);
        ports.add(52235);
        ports.add(9088);
        ports.add(61616);
        ports.add(1275);
        ports.add(8730);
        ports.add(13789);
        ports.add(17657);
        ports.add(7980);
        ports.add(8547);
        ports.add(2323);
        ports.add(10086);
        ports.add(8709);
        ports.add(11470);
        ports.add(42615);
        ports.add(33446);
        ports.add(31232);
        ports.add(2062);
        ports.add(6080);
        ports.add(17130);
        ports.add(1062);
        ports.add(8626);
        ports.add(21290);
        ports.add(9036);
        ports.add(8575);
        ports.add(4);
        ports.add(54662);
        ports.add(8969);
        ports.add(31697);
        ports.add(8794);
        ports.add(1245);
        ports.add(31280);
        ports.add(1616);
        ports.add(2240);
        ports.add(2345);
        ports.add(5566);
        ports.add(3180);
        ports.add(53323);
        ports.add(64028);
        ports.add(20941);
        ports.add(2250);
        ports.add(14);
        ports.add(5577);
        ports.add(1173);
        ports.add(14252);
        ports.add(11);
        ports.add(53281);
        ports.add(49314);
        ports.add(1718);
        ports.add(44699);
        ports.add(19786);
        ports.add(8381);
        ports.add(8680);
        ports.add(4386);
        ports.add(37389);
        ports.add(4480);
        ports.add(8041);
        ports.add(9985);
        ports.add(15289);
        ports.add(554);
        ports.add(1197);
        ports.add(33976);
        ports.add(8726);
        ports.add(20141);
        ports.add(18906);
        ports.add(1155);
        ports.add(10800);
        ports.add(8018);
        ports.add(7890);
        ports.add(10225);
        ports.add(8348);
        ports.add(8043);
        ports.add(33605);
        ports.add(47037);
        ports.add(6654);
        ports.add(33944);
        ports.add(66);
        ports.add(37379);
        ports.add(15160);
        ports.add(34034);
        ports.add(810);
        ports.add(58080);
        ports.add(42132);
        ports.add(8045);
        ports.add(1513);
        ports.add(1120);
        ports.add(9595);
        ports.add(16107);
        ports.add(33987);
        ports.add(65000);
        ports.add(29245);
        ports.add(27421);
        ports.add(2328);
        ports.add(6868);
        ports.add(8384);
        ports.add(37383);
        ports.add(13243);
        ports.add(3328);
        ports.add(12718);
        ports.add(6009);
        ports.add(8048);
        ports.add(10059);
        ports.add(34043);
        ports.add(62798);
        ports.add(22900);
        ports.add(59350);
        ports.add(5060);
        ports.add(54);
        ports.add(61627);
        ports.add(28);
        ports.add(213);
        ports.add(3002);
        ports.add(34061);
        ports.add(22222);
        ports.add(8686);
        ports.add(8436);
        ports.add(8996);
        ports.add(8005);
        ports.add(9057);
        ports.add(9124);
        ports.add(11095);
        ports.add(55595);
        ports.add(7526);
        ports.add(9017);
        ports.add(9188);
        ports.add(1479);
        ports.add(33333);
        ports.add(50);
        ports.add(1380);
        ports.add(18118);
        ports.add(9020);
        ports.add(20507);
        ports.add(52013);
        ports.add(101);
        ports.add(20615);
        ports.add(8774);
        ports.add(5921);
        ports.add(20897);
        ports.add(31);
        ports.add(18204);
        ports.add(27410);
        ports.add(2235);
        ports.add(8508);
        ports.add(8489);
        ports.add(8350);
        ports.add(1999);
        ports.add(65233);
    }
}
