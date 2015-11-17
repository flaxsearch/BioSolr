ontologyApp
.controller('SearchCtrl', ['$scope', '$http', function($scope, $http) {
	
	var self = this;
	
	self.init = function() {
		// Look up the dynamic label fields
		$http({
			method: 'GET',
			url: '/service/dynamicLabelFields'
		}).success(function(data) {
			if (data.length > 0) {
				$scope.dynamicLabels = data;
			} else {
				$scope.dynamicLabels = undefined;
			}
		});
		
		$scope.additionalFields = [];
		$scope.selectedFacetStyle = $scope.facetStyle = "NONE";
		// Kludge so we can use checklist-model for the parent/child label checkboxes
		$scope.efo_child_labels = 'efo_child_labels';
		$scope.efo_parent_labels = 'efo_parent_labels';
	}
	
	self.logError = function(errorMsg) {
		if (window.console) {
			console.log(data.errorMessage);
		}
	}
	
	$scope.changePage = function() {
		var start = 10 * ($scope.currentPage - 1);
		var params = { 
			q: $scope.query, 
			additionalFields: $scope.additionalFields, 
			start: start,
			appliedFilters: $scope.appliedFilters,
			facetStyle: $scope.facetStyle
		}
		self.updateModel(params);
	}
	
	// Event handler to catch search form submit
	$scope.search = function() {
		var params = { 
			q: $scope.query, 
			additionalFields: $scope.additionalFields,
			facetStyle: $scope.facetStyle
		};
		// Clear filters
		$scope.fq = undefined;
		$scope.appliedFilters = undefined;
		self.updateModel(params);
	}
	
	$scope.addFilter = function(field, term) {
		var filter = field + ':"' + term + '"';
		if ($scope.fq) {
			$scope.fq.push(filter);
			$scope.appliedFilters.push({ field: term });
		} else {
			$scope.fq = [ filter ];
			$scope.appliedFilters = [{ field: term}];
		}
		
		var params = { 
			q: $scope.query, 
			additionalFields: $scope.additionalFields, 
			fq: $scope.fq,
			facetStyle: $scope.facetStyle
		};
		self.updateModel(params);
	}
	
	$scope.removeFilter = function(filter) {
		for (var i = 0; i < $scope.fq.length; i ++) {
			if ($scope.fq[i] == filter) {
				$scope.fq.splice(i, 1);
			}
		}
		
		var params = { 
			q: $scope.query, 
			additionalFields: $scope.additionalFields, 
			fq: $scope.fq,
			facetStyle: $scope.facetStyle
		};
		self.updateModel(params);
	}
	
	self.updateModel = function(params) {
		$http({
			method: 'GET',
			url: '/service/search',
			params: params
		}).success(function(data) {
			if (data.error) {
				self.logError(data.errorMessage);
				
				// Error state - show an error message, hide the search results
				$scope.error = 'A server error has occurred - try again later.';
			} else {
				$scope.results = data.results;
				$scope.total = data.totalResults;
				$scope.start = data.start + 1;
				$scope.end = data.start + data.rows;
				$scope.currentPage = (data.start / data.rows) + 1;
				$scope.facets = data.facets;
			}
		});
	}
	
	$scope.hasRelated = function(result) {
		// Use underscore.js's size function
		return _.size(result.relatedLabels) > 0;
	}
	
	$scope.formatRelatedType = function(type) {
		var ret = '';
		// Remove field prefix from start of field, split into
		// remaining words
		var words = type.substr('efo_uri_'.length).split('_');
		// Join words into single string, ignoring last three
		// parts (rel, labels, t)
		for (var i = 0; i < words.length - 3; i ++) {
			ret = ret + (i > 0 ? ' ' : '') + words[i];
		}
		// Capitalise first word
		ret = ret.charAt(0).toUpperCase() + ret.slice(1);
		return ret;
	}
	
	$scope.getFacetLabel = function(label) {
		var ret = label;
		
		if (label == 'efo_child_labels_str') {
			ret = 'Child labels';
		} else if (label == 'efo_labels_str') {
			ret = 'Labels';
		} else if (label == 'facet_labels') {
			ret = 'Top-level labels'
		}
		
		return ret;
	}
	
	$scope.getAppliedFilterLabel = function(filter) {
		return $scope.getFacetLabel(filter.split(':')[0]);
	}
	
	$scope.getAppliedFilterValue = function(filter) {
		return filter.substr(filter.indexOf(':') + 1);
	}
	
	$scope.showTopLevelFacets = function() {
		var ret = $scope.facetStyle == 'NONE';
		
		if (ret && $scope.fq && $scope.fq.length > 0) {
			for (var i = 0; i < $scope.fq.length; i ++) {
				if ($scope.fq[i].split(':')[0] == 'facet_labels') {
					ret = false;
					break;
				}
			}
		}
		
		return ret;
	}
	
	$scope.showSecondLevelFacets = function() {
		return $scope.filtersApplied();
	}
	
	$scope.filtersApplied = function() {
		return $scope.fq && $scope.fq.length > 0;
	}
	
	$scope.updateFacetStyle = function(fs) {
		$scope.facetStyle = fs;
		var params = { 
			q: $scope.query, 
			additionalFields: $scope.additionalFields, 
			fq: $scope.fq,
			facetStyle: $scope.facetStyle
		};
		self.updateModel(params);
	}
	
	// Initialise the page
	self.init();
	
}])
.directive('hierarchy', function(RecursionHelper) {
    return {
        restrict: "E",
        scope: {
        	entry: '=',
        	click: '&click',
        	topFunc: '='
        },
        templateUrl: 'partials/hierarchy.html',
        compile: function(element) {
        	return RecursionHelper.compile(element, function(scope) {
        		// Set whether or not the menu is open, and if the entry has a hierarchy
        		scope.isOpen = false;
        		scope.hasHierarchy = scope.entry && scope.entry.hierarchy && scope.entry.hierarchy.length > 0;
        		scope.noHierarchy = !scope.hasHierarchy;
        	});
        }
    };
})
.factory('RecursionHelper', ['$compile', function($compile){
    return {
        /**
         * Manually compiles the element, fixing the recursion loop.
         * @param element
         * @param [link] A post-link function, or an object with function(s) registered via pre and post properties.
         * @returns An object containing the linking functions.
         */
        compile: function(element, link){
            // Normalize the link parameter
            if(angular.isFunction(link)){
                link = { post: link };
            }

            // Break the recursion loop by removing the contents
            var contents = element.contents().remove();
            var compiledContents;
            return {
                pre: (link && link.pre) ? link.pre : null,
                /**
                 * Compiles and re-adds the contents
                 */
                post: function(scope, element){
                    // Compile the contents
                    if(!compiledContents){
                        compiledContents = $compile(contents);
                    }
                    // Re-add the compiled contents to the element
                    compiledContents(scope, function(clone){
                        element.append(clone);
                    });

                    // Call the post-linking function, if any
                    if(link && link.post){
                        link.post.apply(null, arguments);
                    }
                }
            };
        }
    };
}]);