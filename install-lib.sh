#!/bin/bash

# Install the big-o.nl CompGeom library
# See http://big-o.nl/apps/compgeom/ for original download and API.
mvn install:install-file -DgroupId=nl.big-o -DartifactId=compgeom -Dversion=0.3 -Dfile=contrib/CompGeom-0.3.jar -Dpackaging=jar -DgeneratePom=true

# Install Paul Chew's Voronoi/Delaunay Applet library
# See http://www.cs.cornell.edu/home/chew/Delaunay.html for original page and download link.
mvn install:install-file -DgroupId=edu.cornell.cs.chew -DartifactId=voronoi -Dversion=0.1 -Dfile=contrib/delaunay.jar -Dpackaging=jar -DgeneratePom=true
