function showTooltip() {
    var id = document.location.hash.replace(/^#/, '')
    var tooltips = document.getElementsByClassName('tooltip');
    for (var i = 0; i < tooltips.length; i++) {
        tooltips[i].style.visibility = 'hidden';
    }
    if (id!='') {
        show('tooltip' + id);
    }
}

function show(id) {
    document.getElementById(id).style.visibility = 'visible';
}

function hide(id) {
    document.getElementById(id).style.visibility = 'hidden';
}

function scrollToTooltip() {
    var id = document.location.hash.replace(/^#/, '')
    if (id!='') {
        document.getElementById('tooltip' + id).scrollIntoView();
    }
}