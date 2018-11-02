import React from 'react';
import BaseFormatter from '../baseFormatter';

export default class DefaultGqlFormatter extends BaseFormatter {
    static getQueryString(){
        return 'str';
    }

    value(){
        const cell = this.props.cell;
        const val = cell ? cell.val[0] : {};
        return val.str || '';
    }
}