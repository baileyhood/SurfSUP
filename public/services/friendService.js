angular
  .module('surfSup')
  .service('FriendService', function($http) {

    var searchFriendsUrl = '/user';
    function findFriends() {
      return $http.get(searchFriendsUrl);
    }

    var friendInvitationUrl = '/friend';
    function friendInvitation(username) {
      return $http.post(friendInvitationUrl, username);
    }

    var requestAmtUrl = '/requestAmt';
    function requests() {
      return $http.get(requestAmtUrl);
    }

    var requestListUrl = '/requests';
    function requestList() {
      return $http.get(requestListUrl);
    }

    var denyRequestUrl = '/deny';
    function denyRequest (id) {
      return $http.delete(denyRequestUrl + "/" + id);
    }

    return {
      findFriends: findFriends,
      friendInvitation: friendInvitation,
      requests: requests,
      requestList: requestList,
      denyRequest: denyRequest
    };

  });