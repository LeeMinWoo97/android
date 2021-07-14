/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE BATTERY_STATUS PACKING
package com.MAVLink.common;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;

/**
* Battery information
*/
public class msg_battery_status extends MAVLinkMessage{

    public static final int MAVLINK_MSG_ID_BATTERY_STATUS = 147;
    public static final int MAVLINK_MSG_LENGTH = 36;
    private static final long serialVersionUID = MAVLINK_MSG_ID_BATTERY_STATUS;

    /**
    * Consumed charge, in milliampere hours (1 = 1 mAh), -1: autopilot does not provide mAh consumption estimate
    */
    public int current_consumed;
    
    /**
    * Consumed energy, in 100*Joules (intergrated U*I*dt)  (1 = 100 Joule), -1: autopilot does not provide energy consumption estimate
    */
    public int energy_consumed;
    
    /**
    * Temperature of the battery in centi-degrees celsius. INT16_MAX for unknown temperature.
    */
    public short temperature;
    
    /**
    * Battery voltage of cells, in millivolts (1 = 1 millivolt)
    */
    public int voltages[] = new int[10];
    
    /**
    * Battery current, in 10*milliamperes (1 = 10 milliampere), -1: autopilot does not measure the current
    */
    public short current_battery;
    
    /**
    * Battery ID
    */
    public short id;
    
    /**
    * Function of the battery
    */
    public short battery_function;
    
    /**
    * Type (chemistry) of the battery
    */
    public short type;
    
    /**
    * Remaining battery energy: (0%: 0, 100%: 100), -1: autopilot does not estimate the remaining battery
    */
    public byte battery_remaining;
    

    /**
    * Generates the payload for a mavlink message for a message of this type
    * @return
    */
    public MAVLinkPacket pack(){
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_BATTERY_STATUS;
        
        packet.payload.putInt(current_consumed);
        
        packet.payload.putInt(energy_consumed);
        
        packet.payload.putShort(temperature);
        
        
        for (int i = 0; i < voltages.length; i++) {
            packet.payload.putUnsignedShort(voltages[i]);
        }
                    
        
        packet.payload.putShort(current_battery);
        
        packet.payload.putUnsignedByte(id);
        
        packet.payload.putUnsignedByte(battery_function);
        
        packet.payload.putUnsignedByte(type);
        
        packet.payload.putByte(battery_remaining);
        
        return packet;
    }

    /**
    * Decode a battery_status message into this class fields
    *
    * @param payload The message to decode
    */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.current_consumed = payload.getInt();
        
        this.energy_consumed = payload.getInt();
        
        this.temperature = payload.getShort();
        
        
        for (int i = 0; i < this.voltages.length; i++) {
            this.voltages[i] = payload.getUnsignedShort();
        }
                
        
        this.current_battery = payload.getShort();
        
        this.id = payload.getUnsignedByte();
        
        this.battery_function = payload.getUnsignedByte();
        
        this.type = payload.getUnsignedByte();
        
        this.battery_remaining = payload.getByte();
        
    }

    /**
    * Constructor for a new message, just initializes the msgid
    */
    public msg_battery_status(){
        msgid = MAVLINK_MSG_ID_BATTERY_STATUS;
    }

    /**
    * Constructor for a new message, initializes the message with the payload
    * from a mavlink packet
    *
    */
    public msg_battery_status(MAVLinkPacket mavLinkPacket){
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_BATTERY_STATUS;
        unpack(mavLinkPacket.payload);
    }

                      
    /**
    * Returns a string with the MSG name and data
    */
    public String toString(){
        return "MAVLINK_MSG_ID_BATTERY_STATUS -"+" current_consumed:"+current_consumed+" energy_consumed:"+energy_consumed+" temperature:"+temperature+" voltages:"+voltages+" current_battery:"+current_battery+" id:"+id+" battery_function:"+battery_function+" type:"+type+" battery_remaining:"+battery_remaining+"";
    }
}
        