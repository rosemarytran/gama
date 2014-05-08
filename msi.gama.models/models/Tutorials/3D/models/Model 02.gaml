model model2   

global {
  int nb_cells <-100;
  int environmentSize <-100;
  geometry shape <- cube(environmentSize);	
  init { 
    create cells number: nb_cells { 
      location <- {rnd(environmentSize), rnd(environmentSize), rnd(environmentSize)};       
    } 
  }  
} 
  
species cells skills:[moving3D]{  
	
  reflex move{
  	do move;
  }	                    
  aspect default {
    draw sphere(environmentSize*0.01) color:#blue;   
  }
}

experiment Display  type: gui {
  parameter "Initial number of cells: " var: nb_cells min: 1 max: 1000 category: "Cells" ;
  output {
    display View1 type:opengl ambient_light:10 diffuse_light:100{
      species cells;
    }
  }
}