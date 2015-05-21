package org.jlab.rec.bst.track;

import org.jlab.rec.bst.Constants;
import org.jlab.rec.bst.Geometry;
import org.jlab.rec.bst.trajectory.BSTSwimmer;
import trackfitter.track.Helix;


public class EnergyLossCorr {

	public static final double C = 0.0002997924580;

	public String massHypo = "pion";
	/**
	 *  Field instantiated using the torus and the solenoid
	*/
	public BSTSwimmer bstSwim = new BSTSwimmer();
	
	double cosEntAnglesPlanes[] ;

	private double[][] Points;
	//private double[][] CorrPoints;
	private Track _updatedTrack = new Track();
	
	public Track get_UpdatedTrack() {
		return _updatedTrack;
	}

	public void set_UpdatedTrack(Track updatedTrack) {
		this._updatedTrack = updatedTrack;
	}

	 private Helix OrigTrack;
	
	 
	/**
	 * The constructor
	 * @param trkcand the track candidate
	 */
	public EnergyLossCorr(Track trkcand) {		
		
		if(trkcand==null)
			return;
		
		OrigTrack = new Helix(trkcand.get_Helix().get_dca(), trkcand.get_Helix().get_phi_at_dca() ,trkcand.get_Helix().get_curvature(), 
				trkcand.get_Helix().get_Z0(), trkcand.get_Helix().get_tandip(), null);
		
		init(trkcand);
		
	}
	
	
	
	public void doCorrection(Track trkcand, Geometry geo) {
		double B = bstSwim.Bfield(Points[0][0]/10, Points[0][0]/10, Points[0][0]/10).z();
		double ELossMax = 600; //600Mev 
		double stepSize = 0.001; //1 MeV
		int nbins = (int)ELossMax;
		
		double pt0 = trkcand.get_Pt()+ELossMax*stepSize;// Assumes the max ELoss is 600 MeV
		
		double pt = pt0;
		double  curv = (Constants.LIGHTVEL*Math.abs(B))*Math.signum(this.OrigTrack.get_curvature())/pt;
		
		for(int j = 0; j<nbins; j++) {
			if(Math.abs(this.OrigTrack.get_curvature())<Math.abs(curv)) {
				double correctedCurv = (Constants.LIGHTVEL*Math.abs(B))*Math.signum(this.OrigTrack.get_curvature())/(pt+stepSize);
				trkcand.get_Helix().set_curvature(correctedCurv);				
				trkcand.set_HelicalTrack(trkcand.get_Helix());				
				return;
			}
			pt = pt0-j*stepSize;
		
			double aveCurv = 0;	
			for(int k = 0; k< trkcand.size(); k++) {
				aveCurv+=doEnergyLossCorrection(k, pt);			
			}
			aveCurv/=trkcand.size();
			curv = aveCurv;
		
		}
		
	}
	

	private void init(Track trkcand) {
			
		Points 		= new double[trkcand.size()][3] ;
		//CorrPoints 	= new double[trkcand.size()][3] ;
		
		cosEntAnglesPlanes =  new double[trkcand.size()];
		
		Track trkcandcopy = new Track();
		trkcandcopy.addAll(trkcand);
		trkcandcopy.set_HelicalTrack(trkcand.get_Helix());
		trkcandcopy.set_Helix(trkcand.get_Helix());
		
		this.set_UpdatedTrack(trkcandcopy);
		
		for(int m = 0; m<trkcand.size(); m++) {
			
			Points[m][0] = trkcand.get(m).get_Point().x();
			Points[m][1] = trkcand.get(m).get_Point().y();
			Points[m][2] = trkcand.get(m).get_Point().z();
			
			double x = trkcand.get_Helix().getPointAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).x();
			double ux = trkcand.get_Helix().getTrackDirectionAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).x();
			double y = trkcand.get_Helix().getPointAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).y();
			double uy = trkcand.get_Helix().getTrackDirectionAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).y();
			double z = trkcand.get_Helix().getPointAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).z();
			double uz = trkcand.get_Helix().getTrackDirectionAtRadius(Math.sqrt(Points[m][0]*Points[m][0]+Points[m][1]*Points[m][1])).z();
			
			double cosEntranceAngle = Math.abs((x*ux+y*uy+z*uz)/Math.sqrt(x*x+y*y+z*z));
			cosEntAnglesPlanes[m] = cosEntranceAngle;   
				
			}		

		
	}
	
	
	//? Solve numerically stepping over pt until corr pt matches with fit omega... how much dedx corresponds to obs pt?
	
	private double doEnergyLossCorrection(int m, double pt) {
		
		
		double B = bstSwim.Bfield(Points[m][0]/10, Points[m][0]/10, Points[m][0]/10).z(); // Bfield takes units of cm
		
		double tanL = this.OrigTrack.get_tandip();
		
		// pz = pt/tanL
		double p = pt*Math.sqrt(1+(1./tanL)*(1./tanL));
		
	    double mass = MassHypothesis(massHypo); // assume given mass hypothesis 
	    
	    double beta = p/Math.sqrt(p*p+mass*mass); // use particle momentum
	    double gamma = 1./Math.sqrt(1-beta*beta);

	    double cosEntranceAngle = cosEntAnglesPlanes[m];
	    
	    double s = eMass/mass;
 		//double Wmax = 2.*mass*beta*beta*gamma*gamma/(1.+2.*s*Math.sqrt(1+beta*gamma*beta*gamma)+s*s);
 		double Wmax = 2.*mass*beta*beta*gamma*gamma/(1.+2.*s*gamma+s*s);
 		double I = 0.000000172;

 		double logterm = 2.*mass*beta*beta*gamma*gamma*Wmax/(I*I);

 		double delta = 0.;
 		
 		//double dEdx = 0.0001535*(Constants.detMatZ/Constants.detMatA)*(Math.log(logterm)-2*beta*beta-delta)/(beta*beta);
 		double dEdx = 0.0001535*Constants.detMatZ_ov_A_timesThickn*(Math.log(logterm)-2*beta*beta-delta)/(beta*beta);
 		
 		double tmpPtot = p;
		
	    double tmpEtot = Math.sqrt(MassHypothesis(massHypo)*MassHypothesis(massHypo)+tmpPtot*tmpPtot); 
	    //double tmpEtotCorrected = tmpEtot-dEdx*Constants.LAYRGAP/cosEntranceAngle;
	    double tmpEtotCorrected = tmpEtot-dEdx/cosEntranceAngle;
 	    
	    double tmpPtotCorrSq = tmpEtotCorrected*tmpEtotCorrected-MassHypothesis(massHypo)*MassHypothesis(massHypo); 
	   
 	    double newPt = Math.sqrt(tmpPtotCorrSq/(1+(1./tanL)*(1./tanL)));
 	   
 	    double  newCurv = (Constants.LIGHTVEL*Math.abs(B))*Math.signum(this.OrigTrack.get_curvature())/newPt;
 	    
 	    return newCurv;
 	    
	}
	
	/**
	 *  
	 * @param H a string corresponding to the mass hypothesis - the pion mass hypothesis is the default value
	 * @return the mass value for the given mass hypothesis in GeV/c^2
	 */
    public double MassHypothesis(String H) {
  	   double value = piMass; //default
  	   if(H=="proton")
  		  value = pMass;
  	   if(H=="electron")
  		  value = eMass;
  	   if(H=="pion")
  		  value = piMass;
  	   if(H=="kaon")
  		  value = KMass;
  	   if(H=="muon")
  		  value = muMass;
  	return value;
     }
	
   
	 static double piMass = 0.13957018;
	 static double KMass  = 0.493677;
	 static double muMass = 0.105658369;
	 static double eMass  = 0.000510998;
	 static double pMass  = 0.938272029;
	 
	 
	
	

}