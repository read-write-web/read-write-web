/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 */

function isNeedBrowser() {
    var need = false
    if ($.browser.opera)  var need = true
    var userAgent = navigator.userAgent.toString().toLowerCase();
    if ((userAgent.indexOf('safari') != -1) && !(userAgent.indexOf('chrome') != -1)) {
        need = true //we are in safari
    }
    return need
}

/*
 * Opera and Safari (on OSX at least) only serve a certificate if requested in NEED mode (as opposed to TLS WANT)
 * (looking for clear specs on this distinction). In NEED mode if the browser does not have a certificate or if
 * the client does not send one, the web page just shows an UGLY empty error page that no human would understand.
 * This is horrible user experience. Instead we therefore do the request over AJAX, and show a nicely put together
 * error message instead.
 * todo: Clearly the error message could be built using a function call
 */
function needyLogin(){
    $('#login').submit(function(event){
        logout();
        if (isNeedBrowser()) {
            var url = $('#login').attr('action');
            $('body').load(url+ '#wrapper',{post:'yes'},function(response, status, xhr) {
                if (status == 'error') {
                    alert('You probably don\'t have a WebID enabled Certificate. It is worth getting one.' );
                    return false
                }
            });
            return false
        } else return true
    });
}
