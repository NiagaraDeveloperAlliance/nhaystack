//
// Copyright (c) 2012, J2 Innovations
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Mar 2013  Mike Jarmy  Creation
//
package nhaystack.server;

import java.util.*;
import javax.baja.sys.*;

import haystack.*;
import nhaystack.res.*;

/**
  * Types maps between Haystack types and Baja types.
  */
public abstract class Types
{
    public static BSimple toBajaSimple(HVal val)
    {
        if (val instanceof HStr)
        {
            return BString.make(((HStr) val).val); 
        }
        else if (val instanceof HNum)
        {
            HNum num = (HNum) val;

            if (num.unit == null)
            {
                return BDouble.make(num.val); 
            }
            else
            {
                Unit unit = Resources.getSymbolUnit(num.unit);
                if (unit.quantity.equals("time"))
                    return makeRelTime(num, unit);
            }
        }
        else if (val instanceof HBool)
        {
            return BBoolean.make(((HBool) val).val); 
        }
        
        throw new IllegalStateException("Cannot convert " + 
            val.getClass() + ": " + val);
    }

    private static BRelTime makeRelTime(HNum num, Unit unit)
    {
        if      (unit.name.equals("nanosecond"))        return BRelTime.make((long) (num.val / 1000 / 1000));
        else if (unit.name.equals("microsecond"))       return BRelTime.make((long) (num.val / 1000));
        else if (unit.name.equals("millisecond"))       return BRelTime.make((long) num.val);
        else if (unit.name.equals("hundredths_second")) return BRelTime.make((long) (num.val * 10));
        else if (unit.name.equals("tenths_second"))     return BRelTime.make((long) (num.val * 100));
        else if (unit.name.equals("second"))            return BRelTime.makeSeconds ((int) num.val);
        else if (unit.name.equals("minute"))            return BRelTime.makeMinutes ((int) num.val);
        else if (unit.name.equals("hour"))              return BRelTime.makeHours   ((int) num.val);
        else if (unit.name.equals("day"))               return BRelTime.makeHours   ((int) (num.val * 24));

        else throw new IllegalStateException(
            "Cannot convert " + num + " to RelTime");
    }

    public static HVal fromBajaSimple(BSimple simple)
    {
        if (simple instanceof BString)
        {
            return HStr.make(simple.toString());
        }
        else if (simple instanceof BNumber)
        {
            return HNum.make(((BNumber) simple).getDouble());
        }
        else if (simple instanceof BBoolean)
        {
            return HBool.make(((BBoolean) simple).getBoolean());
        }
        else if (simple instanceof BEnum)
        {
            return HStr.make(((BEnum) simple).getTag());
        }
        else
            throw new IllegalStateException("Cannot convert " + simple.getClass());
    }

    /**
      * Convert the args into a parameter that can be used to invoke the Action.
      */
    public static BValue actionArgsToBaja(HDict args, Action action)
    {
        // NOTE we can't use args.size(), because if args is an HRow,
        // the size() can be non-zero even if args.iterator().hasNext() is false.

        // null
        if (args == null || !args.iterator().hasNext())
        {
            return null;
        }
        // simple
        else if (args.size() == 1)
        {
            Map.Entry e = (Map.Entry) args.iterator().next();
            HVal val = (HVal) e.getValue();
            BSimple simple = toBajaSimple(val);

            if (!simple.getType().is(action.getParameterType()))
                throw new IllegalStateException(
                    "type mismatch: " + simple.getType() + 
                    " is not " + action.getParameterType());

            return simple;
        }
        // complex
        else
        {
            BComplex cpx = (BComplex) action.getParameterDefault();

            // Set each slot in the BComplex to a dict entry.
            // Note that we do not currently support nesting structs within structs.
            Iterator it = args.iterator();
            while (it.hasNext())
            {
                Map.Entry e = (Map.Entry) it.next();
                cpx.set(
                    (String) e.getKey(), 
                    toBajaSimple((HVal) e.getValue()));
            }

            return cpx;
        }
    }
}

