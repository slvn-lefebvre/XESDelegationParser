package org.isep.xes;

import org.deckfour.xes.model.*;
import org.deckfour.xes.model.impl.XAttributeImpl;

public class WorkloadXVisitor extends XVisitor {



    // CLASSE A MODIFIER POUR IMPLEMENTER l'ATTRIBUT WORKLOAD
    public class WorkloadAttribute extends XAttributeImpl {

        protected WorkloadAttribute(String key) {
            super(key);
        }

        @Override
        public String getKey() {
            return super.getKey();
        }
    }

    @Override
    public void visitTracePre(XTrace trace, XLog log) {
        super.visitTracePre(trace, log);
    }

    @Override
    public void visitEventPost(XEvent event, XTrace trace) {
        super.visitEventPost(event, trace);


        XAttributeMap xam = event.getAttributes();

        // CALCUL DE WORKLOAD...
        XAttribute transition = xam.get("lifecycle:transition");
        XAttribute conceptName = xam.get("concept:name");
        XAttribute resource = xam.get("org:resource");

        // FIN CALCUL
        // MEttre a jour Workload attr en fonction du besoin
        WorkloadAttribute wkl = new WorkloadAttribute("");

        // MODICIFCATION DE l'attribute Map
        xam.put("workload", wkl);
        // ENREGISTREMENT MAP
        event.setAttributes(xam);
        //FINI
    }
}
