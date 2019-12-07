clc;
clear all;
close all;

%Importing comma separated edge
edges = csvread('example1.dat');
%edges = csvread('example2.dat');        

%STEP 1: generating the affinity matrix

%Converting Edge list to adjacency matrix
column1 = edges(:,1);
column2 = edges(:,2);

graphed = graph(column1, column2);
%Returns the sparse adjacency matrix for graph 'graphed'.
A = adjacency(graphed); 
%We want to plot this later
G = graph(A, 'OmitSelfLoops');           

%plots the sparsity pattern of matrix A. Nonzero values are colored while zero values are white. The plot displays the number of nonzeros in the matrix, nz = nnz(A).
spy(A);
title('Affinity/Sparsity matrix');
%From here with we can see there are about 4 communities, therefore we set the number of clusters K = 4
K=4;

%STEP 2: defining D and constructing L (the Fiedler vector)

%Defining D: sum(A,2) is column vector containing the sum of each row of A.
D = diag(sum(A,2));    
%%Defining the normalized Laplacian:
L = D^(-0.5)*A*D^(-0.5);
%The Laplacian is simply
Laplacian = D - A;

%STEP 3: getting the k largest eigenvectors and create matrix X

%returns the diagonal matrix eigenVals containing the K Largest Magnitude ('LM') eigenvalues of L on the main diagonal,
%and matrix X whose columns are the corresponding eigenvectors.
[X, eigenVals] = eigs(L, K, 'LM');  

%STEP 4: Form the matrix Y from X by normalizing each of X's rows to have unit length  

Y = X./sqrt(sum(X.^2,2));

%STEP 5 & 6: Taking each row in Y as a point and clustering them into K clusters

%kmeans returns a vector of cluster indices (idx) for each observation  and K cluster centroid locations in the K-by-p matrix C
[idx, C] = kmeans(Y, K, 'MaxIter', 1000);   
                                           
%Further PLOTTING 
%Plotting the sparsity matrix again but with the clusters colored
daColor = hsv(K);   %colormap
figure;
hold on;
for i = 1 : size(A)
  point = idx(i, 1);
  for j = 1 : size(A)
    if A(i, j) == 1
        plot(j, i, 'color', daColor(point, :), 'marker', '.');
    end  
  end  
end
hold off;
title('Affinity/Sparsity matrix (with the clusters highlighted)');

%Plotting the graph G
figure;
P1 = plot(G, 'layout', 'force', 'Marker', '.', 'MarkerSize', 10);
title('Graph');
axis equal;

%Plotting the graph with the clusters highlighted
figure;
P2 = plot(G, 'layout', 'force', 'Marker', '.', 'MarkerSize', 10);
title('Graph (with the clusters highlighted)');
axis equal;
for i = 1: K
    highlight(P2, find(idx==i), 'NodeColor', daColor(i,:))
end

%Plotting the Fiedler vector

%returns the diagonal matrix eigenVals containing the 2 smallest real ('sa') eigenvalues of Laplacian on the main diagonal,
%and matrix eigenVecs whose columns are the corresponding eigenvectors.
[eigenVecs,eigenUals] = eigs(Laplacian, 2, 'sa'); 
%Fiedler is the vector corresponding to the 2nd smallest eigenvalue
Fiedler = eigenVecs(:, 2);                  
sortedFiedler = sort(Fiedler);
figure;
plot(sortedFiedler,'.');
title('Sorted Fiedler vector');