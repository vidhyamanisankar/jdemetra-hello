/*
 * Copyright 2016 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.demetra.hello;

import ec.tstoolkit.arima.ArimaModel;
import ec.tstoolkit.arima.estimation.RegArimaEstimation;
import ec.tstoolkit.arima.estimation.RegArimaModel;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.sarima.SarimaModel;
import ec.tstoolkit.sarima.SarimaModelBuilder;
import ec.tstoolkit.sarima.estimation.GlsSarimaMonitor;
import ec.tstoolkit.ssf.DiffuseFilteringResults;
import ec.tstoolkit.ssf.DiffuseSquareRootInitializer;
import ec.tstoolkit.ssf.Filter;
import ec.tstoolkit.ssf.FilteringResults;
import ec.tstoolkit.ssf.Smoother;
import ec.tstoolkit.ssf.SmoothingResults;
import ec.tstoolkit.ssf.SsfData;
import ec.tstoolkit.ssf.ucarima.SsfUcarima;
import ec.tstoolkit.ucarima.ModelDecomposer;
import ec.tstoolkit.ucarima.SeasonalSelector;
import ec.tstoolkit.ucarima.TrendCycleSelector;
import ec.tstoolkit.ucarima.UcarimaModel;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Jean Palate
 */
public class HighFreq3 {

    public static void main(String[] args) throws IOException {
        int freq = 52;
        // the limit for the current implementation
        TrendCycleSelector tsel = new TrendCycleSelector();
        SeasonalSelector ssel = new SeasonalSelector(freq);
        URL resource = HighFreq2.class.getResource("/uspetroleum.txt");
        Matrix pet=MatrixReader.read(new File(resource.getFile()));
        DataBlock m = pet.column(1);

        ModelDecomposer decomposer = new ModelDecomposer();
        decomposer.add(tsel);
        decomposer.add(ssel);
        SarimaModel arima = new SarimaModelBuilder().createAirlineModel(freq, -.9, -.4);
        
        GlsSarimaMonitor monitor=new GlsSarimaMonitor();
        RegArimaModel<SarimaModel> regarima=new RegArimaModel<>(arima, m);
        RegArimaEstimation<SarimaModel> estimation = monitor.process(regarima);
        
        UcarimaModel ucm = decomposer.decompose(estimation.model.getArima());
        ucm.setVarianceMax(-1);
        System.out.println(ucm);
        System.out.println(new DataBlock(ucm.getComponent(1).getMA().getCoefficients()));
        
        SsfUcarima ssf=new SsfUcarima(ucm);
        Filter filter=new Filter();
        DiffuseFilteringResults fr=new DiffuseFilteringResults(true);
        fr.getFilteredData().setSavingA(true);
        fr.getVarianceFilter().setSavingP(true);
        filter.setInitializer(new DiffuseSquareRootInitializer());
        filter.setSsf(ssf);
        filter.process(new SsfData(m, null), fr);
        Smoother smoother=new Smoother();
        smoother.setCalcVar(false);
        smoother.setSsf(ssf);
        SmoothingResults sr=new SmoothingResults();
        smoother.process(new SsfData(m, null), fr, sr);
        System.out.println(m);
        System.out.println(new DataBlock(sr.component(ssf.cmpPos(0))));
        System.out.println(new DataBlock(sr.component(ssf.cmpPos(1))));
        System.out.println(new DataBlock(sr.component(ssf.cmpPos(3))));
        
    }
}

