clc;
clear all;
close all;

edges = csvread('example1.dat');
%edges = csvread('example2.dat');
K = 0;          %Number of clusters
sigma = 0;     %Scaling parameter, for creating the affinity matrix. Paper says there is a way to choose it
                %automatically. This controls how rapidly the affinity
                %matrix falls off with the distance between the vertices

%Following the algorithm,
%STEP 1: generating the affinity matrix, as per the equation
%for i=1:length(edges(:,1))
%     for j=1:length(edges(:,1))
%        if i==j %Zero if the indexes are the same
%            A(i,j)=0;
%        else
%            A(i,j)=exp(-(edges(i,1)-edges(j,1))^2/(2*sigma^2));
%        end
%    end
%end
%CRAP - this is already a graph, so we can easily construct A like so from
%ADJACENCY MATRIX
column1 = edges(:,1);
column2 = edges(:,2);

graphed = graph(column1, column2);
A = adjacency(graphed); %This is actually easier than the above formula
G = graph(A, 'OmitSelfLoops');           %We want to plot this later

spy(A);
title('Affinity/Sparsity matrix');
%From here with we can see there are about 4 communities, which is why we
%set the number of clusters K = 4
K=4;

%STEP 2: defining D and constructing L (the Fiedler vector)
D = diag(sum(A,2));     %Defining D as per the formula
L = D^(-0.5)*A*D^(-0.5);

%The Laplacian is simply
Laplacian = D - A;

%STEP 3: getting the k largest eigenvectors and create matrix X
[X, eigenVals] = eigs(L, K, 'LM');  %Returns the K largest real magnitude
                                        %eigenvalues in the diagonal of the
                                        %second term (eigenVals), and eigenvector columns in
                                        %the first one (X)

%STEP 4: Form the matrix Y (normalizing each row of X)
Y = X./sqrt(sum(X.^2,2));

%STEP 5 & 6: Taking each row in Y as a point and clustering them into K cluster
%via kmeans
[idx, C] = kmeans(Y, K, 'MaxIter', 100);    %Returns a vector of cluster indices
                                            %for each observation (idx) and
                                            %k cluster centroids (C)
                                           
%Further PLOTTING of other stuff
%Now, plotting the sparsity matrix again but with the clusters colored
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

%Plotting the graph
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
[eigenVecs,eigenVals] = eigs(Laplacian, 2, 'sa');   %Return the two smallest eigenvalues
Fiedler = eigenVecs(:, 2);                  %Fiedler is the vector corresponding to the 2nd smallest eigenvalue
sortedFiedler = sort(Fiedler);
figure;
plot(sortedFiedler,'.');
title('Sorted Fiedler vector');