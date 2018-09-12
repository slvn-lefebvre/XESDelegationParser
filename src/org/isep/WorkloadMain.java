package org.isep;

import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XesXmlSerializer;
import org.isep.xes.ExtractionXVisitor;
import org.isep.xes.WorkloadXVisitor;

import java.io.*;
import java.util.List;

public class WorkloadMain {


    public static void main(String[] args) {

        System.out.println("Trying to parse: " + args[0]);
        File f = new File(args[0]);
        System.out.println(f);
        XesXmlParser xp = new XesXmlParser();

        long start = System.currentTimeMillis();


        if (!xp.canParse(f)) {
            System.out.println("Can't parse " + args[0]);
            System.exit(0);
        }

        // ouverture du fichier de log
        try (FileInputStream fis = new FileInputStream(f)) {

            // Un fichier peut contenir plusieurs logs, mais dans notre cas un seul suffit.
            List<XLog> logs = xp.parse(fis);
            System.out.println(logs.size());
            XLog xl = logs.get(0);
            System.out.println(xl.toString());

            // WorkloadXVisitor calcule la workload par ressource
            WorkloadXVisitor wxv = new WorkloadXVisitor();
            xl.accept(wxv);
            // Processing.....


            XesXmlSerializer xxs = new XesXmlSerializer();

            xxs.serialize(xl, new FileOutputStream("out.xml"));


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}